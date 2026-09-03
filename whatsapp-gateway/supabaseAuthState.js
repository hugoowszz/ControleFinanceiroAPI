/**
 * supabaseAuthState.js
 * 
 * Auth state do Baileys persistido no Supabase Storage.
 * Substitui useMultiFileAuthState (que salva em disco local, apagado no Render Free).
 * 
 * Arquivos salvos no bucket "whatsapp-session" do Supabase Storage:
 *   - creds.json  → credenciais da sessão
 *   - keys/...    → chaves Signal (pre-keys, sessions, etc.)
 */

const { StorageClient } = require('@supabase/storage-js');
const { initAuthCreds, BufferJSON, proto } = require('@whiskeysockets/baileys');

const BUCKET_NAME = 'whatsapp-session';

function criarStorageClient() {
  const supabaseUrl    = process.env.SUPABASE_URL;
  const supabaseKey    = process.env.SUPABASE_SERVICE_KEY;

  if (!supabaseUrl || !supabaseKey) {
    throw new Error('❌ SUPABASE_URL e SUPABASE_SERVICE_KEY são obrigatórios para persistência de sessão.');
  }

  return new StorageClient(`${supabaseUrl}/storage/v1`, {
    apikey: supabaseKey,
    Authorization: `Bearer ${supabaseKey}`
  });
}

async function uploadArquivo(storage, caminho, conteudo) {
  const dados = typeof conteudo === 'string' ? conteudo : JSON.stringify(conteudo, BufferJSON.replacer);
  const blob = Buffer.from(dados, 'utf-8');

  await storage.from(BUCKET_NAME).upload(caminho, blob, {
    contentType: 'application/json',
    upsert: true
  });
}

async function downloadArquivo(storage, caminho) {
  try {
    const { data, error } = await storage.from(BUCKET_NAME).download(caminho);
    if (error || !data) return null;
    const texto = await data.text();
    return JSON.parse(texto, BufferJSON.reviver);
  } catch {
    return null;
  }
}

async function useSupabaseAuthState() {
  const storage = criarStorageClient();

  // Garante que o bucket existe (cria se não existir)
  try {
    await storage.createBucket(BUCKET_NAME, { public: false });
    console.log(`📦 Bucket "${BUCKET_NAME}" criado no Supabase Storage.`);
  } catch {
    // Bucket já existe, ignora
  }

  // Carrega as credenciais salvas ou cria novas
  let creds = await downloadArquivo(storage, 'creds.json');
  if (!creds) {
    creds = initAuthCreds();
    console.log('🆕 Nenhuma sessão encontrada. Iniciando nova sessão...');
  } else {
    console.log('✅ Sessão carregada do Supabase Storage!');
  }

  const state = {
    creds,
    keys: {
      get: async (type, ids) => {
        const data = {};
        await Promise.all(ids.map(async (id) => {
          const valor = await downloadArquivo(storage, `keys/${type}-${id}.json`);
          if (valor) data[id] = valor;
        }));
        return data;
      },
      set: async (data) => {
        const tarefas = [];
        for (const [type, valores] of Object.entries(data)) {
          for (const [id, valor] of Object.entries(valores || {})) {
            if (valor) {
              tarefas.push(uploadArquivo(storage, `keys/${type}-${id}.json`, valor));
            }
          }
        }
        await Promise.all(tarefas);
      }
    }
  };

  const saveCreds = async () => {
    await uploadArquivo(storage, 'creds.json', state.creds);
  };

  return { state, saveCreds };
}

module.exports = { useSupabaseAuthState };
