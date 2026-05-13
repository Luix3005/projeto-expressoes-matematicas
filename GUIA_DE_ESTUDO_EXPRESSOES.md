# Guia de Estudo Expressões

Este guia explica o projeto da **Calculadora de Expressões Matemáticas** usando uma linguagem para iniciantes, mas mantendo a visão técnica correta de como um sistema Full Stack funciona.

O projeto tem três partes principais:

- **Frontend Angular**: a tela que o usuário usa.
- **Backend Spring Boot**: a API que recebe pedidos, calcula e salva dados.
- **Banco H2**: a memória temporária/leve onde o histórico fica guardado.

Uma boa analogia é pensar no sistema como um restaurante:

- O **Angular** é o garçom que recebe o pedido do cliente.
- O **Controller** é a porta da cozinha.
- O **Service Java** é o cozinheiro que prepara o prato.
- O **Repository + H2** é o estoque/caderno onde tudo fica registrado.

---

## 1. Visão Geral do Fluxo: The Big Picture

Quando o usuário digita uma expressão e clica em **Calcular e Salvar**, acontece este caminho:

1. O usuário digita algo no campo da tela, por exemplo:

   ```text
   x * 2 + 10
   ```

2. O Angular guarda esse texto na variável `novaConta` por causa do `[(ngModel)]`.

3. Se a expressão contém `x`, a tela mostra o campo **Valor de X**, também ligado por `[(ngModel)]` à variável `valorX`.

4. O usuário clica no botão **Calcular e Salvar**.

5. O método `btnCalcular()` do `AppComponent` é executado.

6. O `AppComponent` chama o `ExpressionService` do Angular.

7. O `ExpressionService` monta uma requisição HTTP para o backend:

   - `POST /api/calculator` para criar uma nova expressão.
   - `PUT /api/calculator/{id}` para editar/recalcular uma expressão existente.

8. O `ExpressionController` no Java recebe essa requisição.

9. O Controller chama o `ExpressionService` do Java.

10. O Service Java:

    - limpa espaços extras da expressão com `trim()`;
    - registra datas de criação/execução;
    - calcula o resultado usando a biblioteca `exp4j`;
    - monta um objeto `Expression`.

11. O `ExpressionRepository` salva o objeto no banco H2.

12. O backend devolve a expressão salva em formato JSON.

13. O Angular recebe a resposta dentro do `subscribe()`.

14. A tela limpa os campos e recarrega a lista.

15. O componente `Historico` chama `GET /api/calculator/todos` e mostra os dados na tabela.

Em resumo:

```text
Clique no botão
-> AppComponent
-> Angular ExpressionService
-> HTTP
-> Java Controller
-> Java Service
-> exp4j calcula
-> Repository salva no H2
-> JSON volta para Angular
-> Histórico aparece na tabela
```

---

## 2. Frontend Angular

### 2.1. O papel do Componente

No Angular, um **componente** é a união de:

- uma classe TypeScript com dados e métodos;
- um HTML que mostra esses dados;
- opcionalmente um CSS/SCSS para estilo.

No projeto, existem dois componentes importantes:

- `AppComponent`: tela principal da calculadora.
- `Historico`: tabela com o histórico de cálculos.

O `AppComponent` cuida da parte de digitar e calcular:

```ts
novaConta: string = '';
idEmEdicao: number | null = null;
valorX: number | null = null;
```

Essas variáveis representam o estado da tela:

- `novaConta`: texto digitado pelo usuário.
- `valorX`: número usado quando a expressão tem variável `x`.
- `idEmEdicao`: indica se o usuário está criando uma expressão nova ou editando uma existente.

O método principal é:

```ts
btnCalcular() {
  if (!this.novaConta.trim()) return;

  if (this.idEmEdicao) {
    this.service.editar(this.idEmEdicao, this.novaConta, this.valorX ?? undefined).subscribe(...)
  } else {
    this.service.salvar(this.novaConta, this.valorX ?? undefined).subscribe(...)
  }
}
```

Ele faz duas decisões importantes:

- Se `novaConta` está vazia, não faz nada.
- Se `idEmEdicao` existe, edita. Caso contrário, cria uma nova expressão.

### 2.2. O papel do Angular Service

O arquivo `expression.service.ts` centraliza as chamadas HTTP.

Isso é importante porque o componente não precisa saber detalhes de URL, `HttpParams`, `GET`, `POST`, `PUT` ou `DELETE`. Ele apenas chama métodos com nomes claros:

- `listar(...)`
- `salvar(...)`
- `editar(...)`
- `excluir(...)`

Analogia: o componente é o cliente pedindo "quero salvar essa expressão"; o service é quem sabe o endereço exato da API e como enviar o pedido.

Exemplo do método `salvar`:

```ts
salvar(expressao: string, valorX?: number): Observable<Expression> {
  let params = new HttpParams().set('textoDaConta', expressao);

  if (valorX !== undefined && valorX !== null) {
    params = params.set('valorX', valorX.toString());
  }

  return this.http.post<Expression>(this.apiUrl, null, { params });
}
```

Aqui o Angular envia a expressão como parâmetro de requisição:

```text
POST http://localhost:8080/api/calculator?textoDaConta=x*2&valorX=5
```

### 2.3. Como o `ngModel` trabalha

O `[(ngModel)]` faz uma ligação de mão dupla entre HTML e TypeScript.

Exemplo:

```html
<input [(ngModel)]="novaConta">
```

Isso significa:

- quando o usuário digita no input, `novaConta` é atualizada;
- se o TypeScript mudar `novaConta`, o input também muda na tela.

Por isso, quando o método `finalizarAcao()` faz:

```ts
this.novaConta = '';
this.valorX = null;
```

os campos da tela ficam limpos.

### 2.4. Como o `EventEmitter` e o `aoEditar` trabalham

O componente `Historico` é um componente filho. Ele mostra a tabela e tem o botão de editar.

Mas quem controla o formulário principal é o `AppComponent`, que é o pai.

Então o filho precisa avisar o pai:

> "O usuário clicou para editar este item aqui."

Para isso existe:

```ts
@Output() aoEditar = new EventEmitter<Expression>();
```

Quando o usuário clica no botão de editar, o filho executa:

```ts
this.aoEditar.emit(item);
```

No HTML do pai, existe:

```html
<app-historico (aoEditar)="prepararEdicao($event)"></app-historico>
```

Isso quer dizer:

- quando o filho emitir `aoEditar`;
- o pai chama `prepararEdicao($event)`;
- `$event` é a expressão enviada pelo filho.

O método do pai então preenche o formulário:

```ts
prepararEdicao(item: Expression) {
  this.idEmEdicao = item.id ?? null;
  this.novaConta = item.expression;
}
```

---

## 3. Porta de Entrada: Controller Java

O arquivo `ExpressionController.java` é a porta HTTP do backend.

Ele usa:

```java
@RestController
@RequestMapping("/api/calculator")
@CrossOrigin(origins = "*")
public class ExpressionController {
```

### 3.1. `@RestController`

Indica que essa classe recebe requisições HTTP e devolve dados, normalmente em JSON.

Quando um método retorna um objeto `Expression`, o Spring transforma esse objeto automaticamente em JSON.

### 3.2. `@RequestMapping("/api/calculator")`

Define o caminho base da API.

Todas as rotas dessa classe começam com:

```text
/api/calculator
```

Exemplos:

- `POST /api/calculator`
- `GET /api/calculator/todos`
- `PUT /api/calculator/{id}`
- `DELETE /api/calculator/{id}`

### 3.3. `@CrossOrigin(origins = "*")`

O frontend Angular roda em uma origem, geralmente:

```text
http://localhost:4200
```

O backend Spring Boot roda em outra:

```text
http://localhost:8080
```

Por segurança, navegadores bloqueiam chamadas entre origens diferentes se o backend não permitir. Isso se chama política de CORS.

O `@CrossOrigin(origins = "*")` diz:

> "Aceito requisições vindas de qualquer origem."

Para desenvolvimento, isso facilita bastante. Em produção, o ideal seria restringir para o domínio real do frontend.

### 3.4. Como o Controller recebe dados

No método `salvar`:

```java
@PostMapping
public Expression salvar(
    @RequestParam String textoDaConta,
    @RequestParam(required = false) Double valorX
) {
    return service.salvar(textoDaConta, valorX);
}
```

O Spring pega os parâmetros da URL:

```text
?textoDaConta=x*2&valorX=5
```

E coloca nos argumentos:

- `textoDaConta = "x*2"`
- `valorX = 5`

Depois o Controller delega para o Service:

```java
service.salvar(textoDaConta, valorX)
```

O Controller não calcula. Ele só recebe, organiza a entrada e chama a camada certa.

---

## 4. O Cérebro: Service Java

O arquivo `ExpressionService.java` contém a regra de negócio.

Ele responde perguntas como:

- Como salvar uma expressão?
- Como editar uma expressão?
- Como calcular uma string matemática?
- Como filtrar o histórico?

### 4.1. `@Service`

```java
@Service
public class ExpressionService {
```

Essa anotação registra a classe no Spring como uma classe de serviço.

Assim, o Spring consegue criar e entregar essa classe para outras partes do sistema, como o Controller.

### 4.2. Salvando uma expressão

```java
public Expression salvar(String textoDaConta, Double valorX) {
    Expression entidade = new Expression();
    String contaLimpa = textoDaConta.trim();

    entidade.setExpression(contaLimpa);
    entidade.setCreatedAt(LocalDateTime.now());
    entidade.setLastExecutedAt(LocalDateTime.now());
    entidade.setCreatedBy("Luiz Felipe");

    entidade.setResult(calcularLogica(contaLimpa, valorX));

    return repository.save(entidade);
}
```

Passo a passo:

1. Cria um objeto Java vazio:

   ```java
   Expression entidade = new Expression();
   ```

2. Remove espaços extras do início e fim:

   ```java
   String contaLimpa = textoDaConta.trim();
   ```

3. Preenche os campos da entidade:

   - expressão;
   - data de criação;
   - data da última execução;
   - criador;
   - resultado calculado.

4. Salva no banco:

   ```java
   repository.save(entidade)
   ```

### 4.3. Como a `exp4j` calcula a String

O método responsável é:

```java
private Double calcularLogica(String expressao, Double valorX) {
    try {
        String expressaoFormatada = expressao.toLowerCase();
        ExpressionBuilder builder = new ExpressionBuilder(expressaoFormatada);

        if (expressaoFormatada.contains("x")) {
            builder.variable("x");
            net.objecthunter.exp4j.Expression e = builder.build();
            e.setVariable("x", (valorX != null) ? valorX : 0.0);
            return e.evaluate();
        } else {
            return builder.build().evaluate();
        }
    } catch (Exception e) {
        System.out.println("Erro ao calcular [" + expressao + "]: " + e.getMessage());
        return null;
    }
}
```

Vamos quebrar isso em partes.

Primeiro, a expressão vira minúscula:

```java
String expressaoFormatada = expressao.toLowerCase();
```

Isso ajuda porque `X` e `x` passam a ser tratados como `x`.

Depois, a biblioteca recebe a string:

```java
ExpressionBuilder builder = new ExpressionBuilder(expressaoFormatada);
```

Pense no `ExpressionBuilder` como um tradutor. Ele pega um texto como:

```text
x * 2 + 10
```

E transforma em uma estrutura matemática que o Java consegue calcular.

Se a expressão contém `x`, o código informa para a biblioteca que `x` é uma variável válida:

```java
builder.variable("x");
```

Sem isso, a `exp4j` olharia para a letra `x` e poderia dizer:

> "Não sei o que é isso."

Depois, a expressão é construída:

```java
net.objecthunter.exp4j.Expression e = builder.build();
```

Em seguida, o valor real de `x` é colocado:

```java
e.setVariable("x", (valorX != null) ? valorX : 0.0);
```

Isso significa:

- se o frontend mandou `valorX`, usa esse valor;
- se não mandou, usa `0.0`.

Exemplo:

```text
Expressão: x * 2 + 10
valorX: 5
```

A biblioteca entende como:

```text
5 * 2 + 10
```

Resultado:

```text
20
```

Por fim:

```java
return e.evaluate();
```

O `evaluate()` é o momento em que a conta realmente é executada.

Se não existe `x`, o processo é mais simples:

```java
return builder.build().evaluate();
```

### 4.4. Tratamento de erro

Se o usuário digitar algo inválido, como:

```text
10 + *
```

a `exp4j` lança uma exceção. O código captura essa exceção:

```java
catch (Exception e) {
    System.out.println("Erro ao calcular [" + expressao + "]: " + e.getMessage());
    return null;
}
```

Nesse caso, o resultado salvo fica `null`.

Isso evita que o backend inteiro quebre por causa de uma expressão inválida.

---

## 5. A Memória: Repository e H2

### 5.1. A entidade `Expression`

O arquivo `Expression.java` representa uma linha da tabela no banco.

```java
@Entity
public class Expression {
```

`@Entity` diz ao JPA:

> "Essa classe deve virar uma tabela no banco de dados."

Campos principais:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

private String expression;
private Double result;
private LocalDateTime createdAt;
private LocalDateTime lastExecutedAt;
private String createdBy;
```

Cada campo vira uma coluna:

| Campo Java | Significado |
| --- | --- |
| `id` | Identificador único |
| `expression` | Texto da expressão matemática |
| `result` | Resultado calculado |
| `createdAt` | Data/hora de criação |
| `lastExecutedAt` | Data/hora da última execução |
| `createdBy` | Nome de quem criou |

### 5.2. O Repository

O arquivo `ExpressionRepository.java` é a ponte com o banco.

```java
public interface ExpressionRepository
    extends JpaRepository<Expression, Long>, JpaSpecificationExecutor<Expression> {
```

Essa linha é poderosa.

`JpaRepository<Expression, Long>` quer dizer:

- a entidade gerenciada é `Expression`;
- o tipo do ID é `Long`.

Só por herdar de `JpaRepository`, o projeto já ganha métodos prontos:

- `save()`
- `findAll()`
- `findById()`
- `deleteById()`

`JpaSpecificationExecutor<Expression>` permite criar filtros dinâmicos, ou seja, buscar combinando termo, criador e data conforme o usuário preencher ou não os campos.

### 5.3. Como um objeto Java vira linha no H2

Quando o código chama:

```java
repository.save(entidade);
```

o Spring Data JPA olha para o objeto `Expression` e monta comandos SQL por baixo dos panos.

Você escreve Java:

```java
entidade.setExpression("10 + 5");
entidade.setResult(15.0);
```

O JPA transforma isso em algo equivalente a:

```sql
insert into expression (expression, result, created_at, last_executed_at, created_by)
values ('10 + 5', 15.0, ..., ..., 'Luiz Felipe');
```

Você não precisou escrever esse SQL manualmente porque o JPA faz esse trabalho.

O H2 é o banco que guarda esses registros. Ele é muito usado em estudos, testes e desafios técnicos porque é leve e simples de configurar.

---

## 6. Lógica de Auditoria e Filtros

### 6.1. Por que existem `createdAt` e `lastExecutedAt`

Esses dois campos parecem parecidos, mas respondem perguntas diferentes.

`createdAt` responde:

> "Quando essa expressão nasceu no sistema?"

Esse valor é definido apenas na criação.

`lastExecutedAt` responde:

> "Quando essa expressão foi calculada ou recalculada pela última vez?"

Esse valor muda quando a expressão é editada ou executada novamente.

Exemplo:

1. Você cria `10 + 5` às 10:00.
2. `createdAt = 10:00`.
3. `lastExecutedAt = 10:00`.
4. Às 10:30, você executa/edita de novo.
5. `createdAt` continua 10:00.
6. `lastExecutedAt` vira 10:30.

Isso é uma forma simples de auditoria. Auditoria significa guardar informações que ajudam a entender o histórico de uma ação.

### 6.2. Como a listagem funciona

No Angular, o histórico chama:

```ts
this.service.listar(
  this.paginaAtual,
  this.itensPorPagina,
  this.termoBusca,
  this.dataFiltro,
  this.criadorFiltro
)
```

O Angular monta parâmetros como:

```text
GET /api/calculator/todos?page=0&size=10&termo=10&dataStr=2026-05-12&criador=Luiz
```

No Java, o Controller recebe:

```java
@RequestParam(required = false) String termo,
@RequestParam(required = false) String dataStr,
@RequestParam(required = false) String criador
```

E chama:

```java
service.listar(termo, dataStr, criador, pageable);
```

### 6.3. Limpeza dos filtros

Antes de filtrar, o Service chama:

```java
private String limparFiltro(String valor) {
    if (valor == null || valor.trim().isEmpty() || valor.equals("undefined") || valor.equals("null")) {
        return null;
    }
    return valor.trim();
}
```

Isso evita que valores vazios ou textos estranhos como `"undefined"` sejam usados na busca.

### 6.4. Filtro por data

Se o usuário escolhe uma data, por exemplo:

```text
2026-05-12
```

o backend transforma essa data em um intervalo do dia inteiro:

```java
inicio = data.atStartOfDay();
fim = data.atTime(LocalTime.MAX);
```

Ou seja:

```text
2026-05-12 00:00:00
até
2026-05-12 23:59:59.999...
```

Depois o filtro usa:

```java
cb.between(root.get("createdAt"), inicio, fim)
```

Isso busca expressões criadas dentro daquele dia.

### 6.5. Filtro por termo e criador

O método `criarFiltro` monta filtros com `Specification`.

Quando existe `termo`, ele busca tanto na expressão quanto no criador:

```java
Predicate porExpressao = cb.like(cb.lower(root.get("expression")), texto);
Predicate porCriador = cb.like(cb.lower(root.get("createdBy")), texto);
filtros.add(cb.or(porExpressao, porCriador));
```

Isso significa:

> "Traga registros em que a expressão contém o termo OU o criador contém o termo."

Quando existe o filtro específico `criador`, ele adiciona:

```java
filtros.add(cb.like(cb.lower(root.get("createdBy")), "%" + criadorBusca.toLowerCase() + "%"));
```

No final, todos os filtros são combinados com `AND`:

```java
return cb.and(filtros.toArray(new Predicate[0]));
```

Ou seja, se o usuário preencher termo, data e criador, o registro precisa obedecer aos três filtros.

---

## 7. Glossário para Iniciantes

### Angular

**Componente**

Parte visual e lógica da tela. É como uma peça de Lego da interface.

**Service Angular**

Classe usada para centralizar regras compartilhadas ou chamadas HTTP. No projeto, ele conversa com a API Java.

**`ngModel`**

Recurso do Angular Forms que liga um input do HTML a uma variável do TypeScript.

**Two-way data binding**

Ligação de mão dupla. A tela atualiza a variável, e a variável atualiza a tela.

**`EventEmitter`**

Ferramenta usada por um componente filho para avisar algo ao componente pai.

**`@Output`**

Marca um evento que sai do componente filho para o pai.

**Observable**

Objeto que representa uma resposta assíncrona. Uma chamada HTTP não volta imediatamente, então o Angular usa `Observable` para representar "algo que vai chegar depois".

**`subscribe()`**

Método usado para escutar o resultado de um `Observable`.

Analogia: você faz um pedido e deixa seu número. O `subscribe()` é o lugar onde você diz o que fazer quando o pedido chegar.

**HTTP**

Protocolo de comunicação entre frontend e backend. É a "língua" usada para pedir e devolver dados pela web.

**JSON**

Formato de dados muito usado em APIs. Parece um objeto JavaScript:

```json
{
  "id": 1,
  "expression": "10 + 5",
  "result": 15
}
```

### Spring Boot / Java

**Controller**

Camada que recebe requisições HTTP. É a porta de entrada do backend.

**Service**

Camada onde ficam as regras de negócio. No projeto, é onde a expressão é calculada e preparada para salvar.

**Repository**

Camada que conversa com o banco de dados.

**Entidade**

Classe Java que representa uma tabela do banco. No projeto, `Expression` é uma entidade.

**JPA**

Tecnologia Java que ajuda a transformar objetos em registros de banco de dados.

**Spring Data JPA**

Ferramenta do Spring que simplifica o uso de JPA e cria vários métodos de banco automaticamente.

**Injeção de Dependência**

É quando uma classe recebe outra classe de que precisa, em vez de criar tudo manualmente.

No projeto:

```java
@Autowired
private ExpressionService service;
```

O Controller precisa do Service. O Spring entrega esse Service automaticamente.

Analogia: em vez de o cozinheiro fabricar a própria panela, a cozinha já entrega a panela certa para ele.

**`@Autowired`**

Anotação que pede ao Spring para injetar uma dependência automaticamente.

**`@Entity`**

Indica que uma classe Java representa uma tabela do banco.

**`@Id`**

Indica qual campo é a chave primária da tabela.

**`@GeneratedValue`**

Indica que o banco/Spring deve gerar o ID automaticamente.

**`Page`**

Representa uma página de resultados. Útil quando existem muitos registros e você não quer trazer tudo de uma vez.

**`Pageable`**

Objeto que informa qual página e quantos itens por página devem ser buscados.

**Specification**

Forma de montar consultas dinâmicas no JPA. É útil quando os filtros são opcionais.

**Predicate**

Uma condição de busca. Exemplo: "createdBy contém Luiz" ou "createdAt está entre duas datas".

**CORS**

Regra de segurança do navegador que controla se um frontend pode chamar um backend em outro endereço.

### Banco de Dados

**H2**

Banco de dados leve, muito usado em projetos de estudo, testes e desafios técnicos.

**Tabela**

Estrutura do banco onde dados são guardados em linhas e colunas.

**Linha**

Um registro dentro da tabela. Uma expressão salva vira uma linha.

**Coluna**

Um campo da tabela, como `expression`, `result` ou `createdAt`.

**Chave primária**

Campo que identifica um registro de forma única. No projeto, é o `id`.

---

## 8. Resumo Técnico Para Entrevista

Este projeto é uma aplicação Full Stack em que o Angular coleta uma expressão matemática, envia para uma API Spring Boot via HTTP, o backend calcula usando `exp4j`, persiste o resultado no banco H2 usando Spring Data JPA e devolve os dados para o frontend exibir no histórico.

A arquitetura está dividida em camadas:

- **Angular Component**: controla tela, inputs e eventos.
- **Angular Service**: centraliza chamadas HTTP.
- **Controller Java**: expõe endpoints REST.
- **Service Java**: concentra regra de negócio e cálculo.
- **Repository**: abstrai acesso ao banco.
- **H2/JPA**: armazena entidades como linhas de tabela.

Essa separação deixa o código mais organizado, mais fácil de testar e mais fácil de explicar em um desafio técnico.
