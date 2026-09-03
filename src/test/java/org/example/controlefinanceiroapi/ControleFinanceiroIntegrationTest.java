package org.example.controlefinanceiroapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controlefinanceiroapi.dto.LoginRequest;
import org.example.controlefinanceiroapi.entity.Categoria;
import org.example.controlefinanceiroapi.entity.Movimentacao;
import org.example.controlefinanceiroapi.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ControleFinanceiroIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private String registrarELogar(String nome, String email, String senha) throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(senha);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usuario)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, senha);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return responseNode.get("token").asText();
    }

    @Test
    @DisplayName("Deve registrar usuário, autenticar via JWT e acessar rotas protegidas")
    void testRegistroELogin() throws Exception {
        String token = registrarELogar("Alice Silva", "alice@example.com", "senha123");

        mockMvc.perform(get("/categorias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Não deve permitir acesso a rotas protegidas sem token ou com token inválido")
    void testAcessoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/categorias"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/movimentacoes"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve garantir isolamento de dados entre Usuario A e Usuario B")
    void testIsolamentoDeDadosEntreUsuarios() throws Exception {
        String tokenA = registrarELogar("Usuario A", "usuarioa@example.com", "senhaA123");

        Categoria catA = new Categoria();
        catA.setNome("Alimentacao A");
        MvcResult catResultA = mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Alimentacao A"))
                .andReturn();

        Long catIdA = objectMapper.readTree(catResultA.getResponse().getContentAsString()).get("id").asLong();

        // Usuario A cria movimentação (saída)
        Movimentacao movA = new Movimentacao();
        movA.setCategoria_id(catIdA);
        movA.setDescricao("Supermercado");
        movA.setValor(250.75);
        movA.setTipo("SAIDA");
        movA.setFormaPagamento("Cartao de Credito");
        movA.setData(LocalDate.now());

        MvcResult movResultA = mockMvc.perform(post("/movimentacoes")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.descricao").value("Supermercado"))
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andReturn();

        Long movIdA = objectMapper.readTree(movResultA.getResponse().getContentAsString()).get("id").asLong();

        String tokenB = registrarELogar("Usuario B", "usuariob@example.com", "senhaB123");

        // Usuario B listando categorias (deve estar vazia)
        mockMvc.perform(get("/categorias")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Usuario B listando movimentações (deve estar vazio)
        mockMvc.perform(get("/movimentacoes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Usuario B tentando obter categoria do Usuario A por ID (deve retornar 404)
        mockMvc.perform(get("/categorias/" + catIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Usuario B tentando obter movimentação do Usuario A por ID (deve retornar 404)
        mockMvc.perform(get("/movimentacoes/" + movIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Usuario B tentando editar movimentação do Usuario A (deve retornar 404)
        Movimentacao movUpdate = new Movimentacao();
        movUpdate.setDescricao("Invasor");
        movUpdate.setValor(10.0);
        movUpdate.setTipo("SAIDA");
        movUpdate.setFormaPagamento("PIX");
        mockMvc.perform(put("/movimentacoes/" + movIdA)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movUpdate)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/movimentacoes/" + movIdA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Usuario B tentando criar movimentação usando categoria do Usuario A (deve falhar com 400)
        Movimentacao movB = new Movimentacao();
        movB.setCategoria_id(catIdA);
        movB.setDescricao("Gasto com categoria alheia");
        movB.setValor(50.0);
        movB.setTipo("SAIDA");
        movB.setFormaPagamento("Dinheiro");
        mockMvc.perform(post("/movimentacoes")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movB)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve permitir CRUD completo de Movimentacao (ENTRADA e SAIDA) para o próprio usuário")
    void testCrudCompletoProprioUsuario() throws Exception {
        String token = registrarELogar("Carlos", "carlos@example.com", "senha123");

        // Criar Categoria
        Categoria cat = new Categoria();
        cat.setNome("Transporte");
        MvcResult catRes = mockMvc.perform(post("/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cat)))
                .andExpect(status().isCreated())
                .andReturn();
        Long catId = objectMapper.readTree(catRes.getResponse().getContentAsString()).get("id").asLong();

        // Criar Movimentação SAIDA
        Movimentacao movSaida = new Movimentacao();
        movSaida.setCategoria_id(catId);
        movSaida.setDescricao("Combustivel");
        movSaida.setValor(150.0);
        movSaida.setTipo("SAIDA");
        movSaida.setFormaPagamento("Cartao de Debito");
        movSaida.setData(LocalDate.now());

        MvcResult movRes = mockMvc.perform(post("/movimentacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movSaida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andReturn();
        Long movId = objectMapper.readTree(movRes.getResponse().getContentAsString()).get("id").asLong();

        // Criar Movimentação ENTRADA
        Movimentacao movEntrada = new Movimentacao();
        movEntrada.setDescricao("Salario");
        movEntrada.setValor(3000.0);
        movEntrada.setTipo("ENTRADA");
        movEntrada.setFormaPagamento("PIX");
        movEntrada.setData(LocalDate.now());

        mockMvc.perform(post("/movimentacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movEntrada)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.valor").value(3000.0));

        // Buscar por ID
        mockMvc.perform(get("/movimentacoes/" + movId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Combustivel"))
                .andExpect(jsonPath("$.valor").value(150.0));

        // Editar
        movSaida.setDescricao("Gasolina Aditivada");
        movSaida.setValor(180.0);
        mockMvc.perform(put("/movimentacoes/" + movId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movSaida)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Gasolina Aditivada"))
                .andExpect(jsonPath("$.valor").value(180.0));

        // Excluir
        mockMvc.perform(delete("/movimentacoes/" + movId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verificar que foi excluído
        mockMvc.perform(get("/movimentacoes/" + movId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // Excluir Categoria
        mockMvc.perform(delete("/categorias/" + catId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
