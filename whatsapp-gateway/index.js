require('dotenv').config();
const {
  default: makeWASocket,
  useMultiFileAuthState,
  DisconnectReason,
  fetchLatestBaileysVersion
} = require('@whiskeysockets/baileys');
const qrcode = require('qrcode-terminal');
const axios = require('axios');
const pino = require('pino');

const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const API_EMAIL = process.env.API_EMAIL;
const API_SENHA = process.env.API_SENHA;

let tokenJwtAtual = process.env.API_AUTH_TOKEN || null;

// ✅ Evita múltiplas reconexões simultâneas
let reconectando = false;

async function autenticarNaApiSpringBoot() {
  if (!API_EMAIL || !API_SENHA) {
    if (tokenJwtAtual) { console.log('🔑 Usando token JWT fixo do .env'); return; }
    return;
  }
  try {
    console.log(`🔐 Autenticando na API Spring Boot...`);
    const response = await axios.post(`${API_BASE_URL}/auth/login`, { email: API_EMAIL, senha: API_SENHA });
    if (response.data?.token) {
      tokenJwtAtual = response.data.token;
      console.log(`✅ Autenticado como: ${response.data.nome || API_EMAIL}`);
    }
  } catch (error) {
    console.error('❌ Falha ao autenticar:', error.response?.data || error.message);
  }
}

function parserLocal(texto) {
  const hoje = new Date().toISOString().split('T')[0];
  const limpo = texto.toLowerCase().trim();
  const palavrasEntrada = ['recebi', 'ganhei', 'entrou', 'salário', 'salario', 'renda', 'depósito', 'deposito'];
  const ehEntrada = palavrasEntrada.some(p => limpo.includes(p));
  const regexValor = /(?:r\$\s*)?(\d+(?:[.,]\d{1,2})?)\s*(?:reais|conto)?/i;
  const matchValor = limpo.match(regexValor);
  if (!matchValor) return null;
  const valor = parseFloat(matchValor[1].replace(',', '.'));
  if (isNaN(valor) || valor <= 0) return null;
  let descricao = limpo
    .replace(matchValor[0], '')
    .replace(/\b(recebi|ganhei|entrou|gastei|comprei|paguei|no|na|de|em|com|reais|conto|pix|cartao|dinheiro|debito|credito|hoje|ontem|salario|salário)\b/gi, '')
    .replace(/\s+/g, ' ').trim();
  if (!descricao || descricao.length < 2) descricao = ehEntrada ? 'Entrada' : 'Gasto diverso';
  let formaPagamento = 'Dinheiro';
  if (limpo.includes('pix')) formaPagamento = 'PIX';
  else if (limpo.includes('crédito') || limpo.includes('credito')) formaPagamento = 'Cartão de Crédito';
  else if (limpo.includes('débito') || limpo.includes('debito')) formaPagamento = 'Cartão de Débito';
  return { descricao: descricao.charAt(0).toUpperCase() + descricao.slice(1), valor, tipo: ehEntrada ? 'ENTRADA' : 'SAIDA', formaPagamento, data: hoje };
}

async function processarMensagem(texto) {
  const hoje = new Date().toISOString().split('T')[0];
  if (GEMINI_API_KEY.trim().startsWith('AIzaSy')) {
    const prompt = `Você é um assistente financeiro. Data de hoje: ${hoje}. Mensagem: "${texto}"\nSe for movimentação financeira retorne APENAS JSON: {"descricao":"...","valor":0.0,"tipo":"SAIDA","formaPagamento":"Dinheiro","data":"${hoje}"}. tipo="ENTRADA" se recebeu/ganhou dinheiro, "SAIDA" se gastou/pagou. Se NÃO for financeiro retorne: {"invalido":true}`;
    for (const modelo of ['gemini-1.5-flash', 'gemini-2.0-flash']) {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelo}:generateContent?key=${GEMINI_API_KEY.trim()}`;
        const response = await axios.post(url, { contents: [{ parts: [{ text: prompt }] }], generationConfig: { responseMimeType: 'application/json' } }, { headers: { 'Content-Type': 'application/json' } });
        const txt = response.data?.candidates?.[0]?.content?.parts?.[0]?.text;
        if (txt) {
          const dados = JSON.parse(txt);
          if (!dados.invalido && dados.descricao && dados.valor !== undefined) return dados;
          return null;
        }
      } catch (_) {}
    }
  }
  return parserLocal(texto);
}

async function enviarParaBackend(mov) {
  try {
    const headers = { 'Content-Type': 'application/json' };
    if (tokenJwtAtual) headers['Authorization'] = `Bearer ${tokenJwtAtual}`;
    const payload = { descricao: mov.descricao, valor: Number(mov.valor), tipo: mov.tipo || 'SAIDA', formaPagamento: mov.formaPagamento || 'Outros', data: mov.data };
    console.log(`📤 Enviando:`, JSON.stringify(payload));
    const response = await axios.post(`${API_BASE_URL}/movimentacoes`, payload, { headers });
    console.log(`✅ Salvo! Status: ${response.status}`);
    return { sucesso: true };
  } catch (error) {
    console.error('❌ Falha ao salvar:', error.response?.data || error.message);
    return { sucesso: false };
  }
}

async function iniciarWhatsAppGateway() {
  await autenticarNaApiSpringBoot();

  const { state, saveCreds } = await useMultiFileAuthState('./auth_info_baileys');
  const { version } = await fetchLatestBaileysVersion();

  console.log(`🚀 Conectando ao WhatsApp...`);

  const sock = makeWASocket({
    version,
    auth: state,
    logger: pino({ level: 'silent' }),
    printQRInTerminal: false,
    browser: ['Porkito', 'Chrome', '1.0.0'],
    syncFullHistory: false,
    // Evita Bad MAC ao reconectar: retorna mensagem vazia para mensagens antigas
    getMessage: async (key) => {
      return { conversation: '' };
    }
  });

  sock.ev.on('creds.update', saveCreds);

  sock.ev.on('connection.update', ({ connection, lastDisconnect, qr }) => {
    if (qr) {
      console.log('\n📲 Escaneie o QR Code:\n');
      qrcode.generate(qr, { small: true });
    }

    if (connection === 'open') {
      reconectando = false;
      console.log('\n✅ Conectado ao WhatsApp!');
      console.log(`👤 Seu número: ${sock.user?.id?.split(':')[0]?.split('@')[0]}`);
      console.log(`👤 Seu LID   : ${sock.user?.lid?.split(':')[0]?.split('@')[0]}`);
      console.log('📌 Aguardando mensagens de gasto/receita enviadas para você mesmo...\n');
    }

    if (connection === 'close') {
      const code = lastDisconnect?.error?.output?.statusCode;
      const motivo = lastDisconnect?.error?.message || String(code);

      // Sessão encerrada via logout — não reconectar
      if (code === DisconnectReason.loggedOut) {
        console.log('⛔ Sessão encerrada (logout). Apague ./auth_info_baileys e reinicie.');
        return;
      }

      // Conflito: outra sessão WhatsApp Web ativa no mesmo número
      // Código 440 = connectionReplaced
      const isConflict = code === 440 || motivo.toLowerCase().includes('conflict');
      if (isConflict) {
        if (!reconectando) {
          reconectando = true;
          console.log('\n⚠️  CONFLITO DE SESSÃO DETECTADO!');
          console.log('   Outra instância do WhatsApp Web está ativa.');
          console.log('   👉 No celular: WhatsApp > Aparelhos Conectados > remova outras sessões.');
          console.log('   Tentando reconectar em 30 segundos...\n');
          setTimeout(() => {
            reconectando = false;
            iniciarWhatsAppGateway();
          }, 30000);
        }
        return;
      }

      // Outros erros: reconecta em 5 segundos
      if (!reconectando) {
        reconectando = true;
        console.log(`⚠️  Conexão perdida (${motivo}). Reconectando em 5 segundos...`);
        setTimeout(() => {
          iniciarWhatsAppGateway();
        }, 5000);
      }
    }
  });

  sock.ev.on('messages.upsert', async ({ messages, type }) => {
    for (const msg of messages) {
      try {
        const remoteJid  = msg.key?.remoteJid || '';
        const fromMe     = msg.key?.fromMe;
        const selfPhone  = sock.user?.id?.split('@')[0]?.split(':')[0] || '';
        const selfLidNum = sock.user?.lid?.split('@')[0]?.split(':')[0] || '';
        const chatAlvo   = remoteJid.split('@')[0].split(':')[0];
        const texto =
          msg.message?.conversation ||
          msg.message?.extendedTextMessage?.text ||
          msg.message?.editedMessage?.message?.protocolMessage?.editedMessage?.conversation ||
          msg.message?.editedMessage?.message?.protocolMessage?.editedMessage?.extendedTextMessage?.text;

        // Log diagnóstico — mostra tudo que chega
        console.log(`[${type}] jid="${remoteJid}" fromMe=${fromMe} alvo="${chatAlvo}" match=${chatAlvo===selfPhone||chatAlvo===selfLidNum} texto="${texto||'(vazio)'}"`);

        // Filtros
        if (!fromMe)                                              continue;
        if (remoteJid.endsWith('@g.us'))                          continue;
        if (remoteJid.endsWith('@newsletter'))                    continue;
        if (chatAlvo !== selfPhone && chatAlvo !== selfLidNum)    continue;
        if (!texto || texto.trim().length === 0)                  continue;
        if (['✅','⚠️','💚','🔴'].some(e => texto.startsWith(e))) continue;

        console.log(`\n📩 Mensagem para você mesmo: "${texto}"`);

        const movimentacao = await processarMensagem(texto.trim());
        if (!movimentacao) { console.log(`ℹ️  Não é movimentação financeira.`); continue; }

        console.log(`🎯 [${movimentacao.tipo}] ${movimentacao.descricao} - R$${movimentacao.valor}`);

        const resultado = await enviarParaBackend(movimentacao);
        const valorFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(movimentacao.valor);
        const emoji = movimentacao.tipo === 'ENTRADA' ? '💚' : '🔴';

        await sock.sendMessage(remoteJid, {
          text: resultado.sucesso
            ? `${emoji} *Movimentação Registrada!*\n\n📝 *Descrição:* ${movimentacao.descricao}\n💰 *Valor:* ${valorFmt}\n📊 *Tipo:* ${movimentacao.tipo === 'ENTRADA' ? '⬆️ Entrada' : '⬇️ Saída'}\n💳 *Pagamento:* ${movimentacao.formaPagamento}\n📅 *Data:* ${movimentacao.data}`
            : `⚠️ Identifiquei (${movimentacao.descricao} - ${valorFmt}) mas houve erro ao salvar na API.`
        });

      } catch (err) {
        console.error('❌ Erro ao processar mensagem:', err.message);
      }
    }
  });
}

iniciarWhatsAppGateway().catch(err => {
  console.error('❌ Erro fatal:', err);
});