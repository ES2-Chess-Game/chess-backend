# chess-backend

API REST do jogo de xadrez, implementada em Java 25 com Spring Boot e Gradle. O backend cria partidas, mantém seu estado durante a execução do processo e valida movimentos antes de atualizar o tabuleiro.

Para o overview e a documentação geral, consulte o [README do chess-docs](https://github.com/ES2-Chess-Game/chess-docs#readme). A interface que consome esta API está documentada no [README do chess-frontend](https://github.com/ES2-Chess-Game/chess-frontend#readme).

## Requisitos e execução

É necessário ter Java 25, conforme a toolchain declarada em `build.gradle`. O repositório inclui o Gradle Wrapper.

Na raiz deste repositório, inicie a aplicação:

```powershell
.\gradlew.bat bootRun
```

A configuração não define outra porta; com o padrão do Spring Boot, a API fica em `http://localhost:8080/api/partidas`.

Para executar os testes existentes:

```powershell
.\gradlew.bat test
```

O teste atual verifica o carregamento do contexto Spring (`contextLoads`).

## Arquitetura

- [`ChessBackendApplication`](src/main/java/br/uff/chess/ChessBackendApplication.java) é o ponto de entrada Spring Boot.
- [`controller/GameController`](src/main/java/br/uff/chess/controller/GameController.java) publica os endpoints e converte exceções do domínio em respostas HTTP.
- [`service/GameService`](src/main/java/br/uff/chess/service/GameService.java) cria e consulta partidas, valida os dados do lance e atualiza o estado.
- [`model`](src/main/java/br/uff/chess/model) representa partida, tabuleiro, posição, peça, cor e status.
- [`dto`](src/main/java/br/uff/chess/dto) define os dados recebidos e as respostas serializadas pela API.
- [`service/validator`](src/main/java/br/uff/chess/service/validator) implementa validação por tipo de peça através de `PieceMoveValidator` e validadores específicos.
- [`config`](src/main/java/br/uff/chess/config) configura autorização HTTP e CORS.

O tabuleiro é uma matriz 8x8 criada na posição inicial. `GameService` guarda cada partida em um `ConcurrentHashMap`, associada a um ID UUID. O mapa está apenas na memória do processo; reiniciar o backend remove todas as partidas. As dependências JPA aparecem comentadas no `build.gradle`, e não há camada de persistência ativa.

Para validar um lance, o serviço escolhe o validador associado ao tipo da peça e simula o resultado para impedir movimentos que deixem o próprio rei em xeque. A geração de lances legais também é usada pela IA, que avalia material e usa minimax com poda alpha-beta. Após cada lance, a partida identifica xeque-mate ou empate por afogamento.

## API HTTP

| Método | Caminho | Comportamento |
| --- | --- | --- |
| `POST` | `/api/partidas` | Cria partida no tabuleiro inicial, com as brancas no primeiro turno. |
| `GET` | `/api/partidas/{id}` | Retorna o estado da partida ou `404` se o ID não existir. |
| `POST` | `/api/partidas/{id}/lances` | Valida e aplica um movimento, retornando o estado atualizado. |
| `POST` | `/api/partidas/{id}/ia?difficulty=EASY\|MEDIUM\|ADVANCED` | A IA escolhe e aplica um lance para o lado da vez usando minimax alpha-beta. As profundidades são 1 (fácil), 2 (médio) e 3 (avançado); se omitido, usa `ADVANCED`. |
| `GET` | `/api/partidas/{id}/movimentos?linha=L&coluna=C` | Retorna as posições de destino válidas para a peça na casa (linha, coluna), ou todos os lances legais se omitidos os parâmetros. |

O corpo de um lance contém coordenadas inteiras de linha e coluna, entre `0` e `7`. Para escolher uma peça ao promover, envie `promocao` com `RAINHA`, `TORRE`, `BISPO` ou `CAVALO`; se omitido, o peão vira rainha:

```json
{
	"origemLinha": 6,
	"origemColuna": 4,
	"destinoLinha": 4,
	"destinoColuna": 4,
	"promocao": "RAINHA"
}
```

A linha `0` representa o topo da matriz e a coluna `0` corresponde à coluna `a` do tabuleiro. A resposta é um `GameDTO` com `id`, `tabuleiro`, `turnoAtual` e `status`; as casas vazias são `null`. Movimentos inválidos retornam HTTP `400`; partidas inexistentes retornam HTTP `404`. Ambos retornam um objeto JSON com a propriedade `erro`.

## Regras implementadas e limites

Há validadores para rei, rainha, torre, bispo, cavalo e peão. O peão pode avançar uma casa, avançar duas casas da posição inicial com o caminho livre, capturar na diagonal e capturar en passant imediatamente após o avanço duplo adversário. Peões na última fileira são promovidos a rainha por padrão; a API também aceita torre, bispo ou cavalo.

O roque é permitido uma vez por lado enquanto rei e torre envolvidos não tiverem se movido, desde que o caminho esteja livre e o rei não atravesse nem termine em uma casa atacada. Todo lance deve deixar o próprio rei fora de xeque; a partida indica xeque, xeque-mate e empate por afogamento. A IA também considera os lances especiais.

A IA é uma implementação base com avaliação material e busca alpha-beta limitada à profundidade da dificuldade escolhida, sem heurísticas posicionais ou histórico. Não há contas, histórico persistido ou banco de dados configurado.

## Segurança e integração com o frontend

`SecurityConfig` permite acesso sem autenticação às rotas `/api/**` e desativa CSRF nessa configuração. As demais rotas exigem autenticação. `CorsConfig` aceita a origem `http://localhost:5173`, usada pelo Vite, e declara os métodos `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`, além de qualquer header. O endereço da API usado pelo frontend está fixado em `http://localhost:8080`.

## Dependências principais

O `build.gradle` configura Java 25, Spring Boot 4.1.0 e Gradle Wrapper. As dependências ativas incluem Spring WebMVC e Spring Security; não há banco de dados configurado.