package br.com.brainvest.api.curriculum;

import br.com.brainvest.api.api.ApiModels.LevelView;
import br.com.brainvest.api.api.ApiModels.OptionView;
import br.com.brainvest.api.api.ApiModels.QuestionView;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CurriculumCatalog {

    public record Option(String id, String text) {}

    public record Question(
            String id,
            String context,
            String prompt,
            String concept,
            List<Option> options,
            String correctOptionId,
            String correctFeedback,
            String incorrectFeedback) {}

    public record Level(
            int id,
            String title,
            String emoji,
            String tone,
            String description,
            String missionTitle,
            String missionBrief,
            String missionSummary,
            List<Question> questions) {}

    private final List<Level> levels = buildLevels();
    private final Map<String, Question> questions = levels.stream()
            .flatMap(level -> level.questions().stream())
            .collect(Collectors.toUnmodifiableMap(Question::id, Function.identity()));

    public List<Level> levels() {
        return levels;
    }

    public Level level(int levelId) {
        return levels.stream()
                .filter(level -> level.id() == levelId)
                .findFirst()
                .orElseThrow(() -> new CurriculumNotFoundException("Nível não encontrado."));
    }

    public Question question(String questionId) {
        Question question = questions.get(questionId);
        if (question == null) throw new CurriculumNotFoundException("Questão não encontrada.");
        return question;
    }

    public int levelIdFor(String questionId) {
        return Integer.parseInt(questionId.substring(1, questionId.indexOf('-')));
    }

    public List<LevelView> publicLevels() {
        return levels.stream().map(level -> new LevelView(
                level.id(), level.title(), level.emoji(), level.tone(), level.description(),
                level.missionTitle(), level.missionBrief(), level.missionSummary(),
                level.questions().stream().map(question -> new QuestionView(
                        question.id(), question.context(), question.prompt(), question.concept(),
                        question.options().stream().map(option -> new OptionView(option.id(), option.text())).toList()
                )).toList()
        )).toList();
    }

    private static List<Option> options(String correctId, String correctText, String distractorOne, String distractorTwo) {
        List<String> distractors = List.of(distractorOne, distractorTwo);
        int distractorIndex = 0;
        java.util.ArrayList<Option> options = new java.util.ArrayList<>();
        for (String id : List.of("A", "B", "C")) {
            options.add(new Option(id, id.equals(correctId) ? correctText : distractors.get(distractorIndex++)));
        }
        return List.copyOf(options);
    }

    private static Question q(String id, String context, String prompt, String concept, String correctId,
                              String correctText, String distractorOne, String distractorTwo,
                              String correctFeedback, String incorrectFeedback) {
        return new Question(id, context, prompt, concept, options(correctId, correctText, distractorOne, distractorTwo),
                correctId, correctFeedback, incorrectFeedback);
    }

    private static Level level(int id, String title, String emoji, String tone, String description,
                               String missionTitle, String missionBrief, String missionSummary, Question... questions) {
        return new Level(id, title, emoji, tone, description, missionTitle, missionBrief, missionSummary, List.of(questions));
    }

    private static List<Level> buildLevels() {
        return List.of(
            level(1, "Minha vida financeira", "🧾", "mint",
                "Entenda para onde seu dinheiro vai e tome decisões melhores no dia a dia.",
                "Missão: o primeiro salário de Lucas",
                "Lucas tem 19 anos, recebe R$ 2.400 líquidos e gasta R$ 1.700 por mês. Ele quer se organizar sem deixar de aproveitar a vida.",
                "Lucas fechou o mes entendendo renda, gastos essenciais, desejos e juros do cartao. A ponte com CPA e clara: antes de produto financeiro, vem diagnostico, orcamento e decisao adequada ao objetivo.",
                q("l1-q1", "Organização do mês", "Qual é o primeiro passo para Lucas descobrir quanto pode guardar?", "Orçamento", "A",
                    "Anotar renda e gastos por um mês", "Investir todo o saldo da conta hoje", "Cancelar todos os momentos de lazer",
                    "Isso. Registrar entradas e saídas mostra o que já está comprometido e quanto cabe nos planos.",
                    "Antes de decidir quanto guardar ou cortar, Lucas precisa saber para onde o dinheiro está indo. Um orçamento começa com esse retrato."),
                q("l1-q2", "Necessidade ou desejo", "A geladeira de Lucas quebrou. Ele também queria trocar o celular, que ainda funciona. Qual gasto tende a ser prioridade?", "Priorização de gastos", "B",
                    "Resolver a geladeira e reavaliar o celular", "Trocar o celular, porque era o plano inicial", "Parcelar os dois sem olhar o orçamento",
                    "Boa. Necessidades urgentes vêm antes de desejos que podem esperar, considerando os recursos disponíveis.",
                    "Um plano pode mudar quando surge uma necessidade essencial. Adiar um desejo ajuda a evitar dívida e manter o orçamento sob controle."),
                q("l1-q3", "Cartão de crédito", "A fatura de Lucas é R$ 1.400 e vence hoje. Ele tem o valor na conta e nenhuma despesa urgente antes do próximo salário. O que tende a evitar juros?", "Pagamento da fatura", "C",
                    "Pagar a fatura integralmente", "Pagar o valor mínimo", "Esperar o próximo mês sem avisar o banco",
                    "Correto. Pagar o total até o vencimento evita financiar o saldo da fatura no crédito rotativo.",
                    "O pagamento mínimo não quita a fatura: o saldo restante pode gerar juros. Como Lucas tem o valor disponível, pagar o total evita esse financiamento."),
                q("l1-q4", "Juros no cartão", "Lucas pagou só parte da fatura. O que acontece com o restante, em geral?", "Crédito rotativo", "A",
                    "Pode entrar no crédito rotativo e gerar juros", "É automaticamente perdoado no mês seguinte", "Vira uma compra parcelada sem custo",
                    "Isso. O saldo não pago pode ser financiado com juros; vale procurar alternativas de renegociação antes que a dívida cresça.",
                    "Pagar menos que o total não elimina a diferença. O saldo pode ser financiado e gerar encargos, então é importante agir cedo."),
                q("l1-q5", "Decisão do mês", "Depois de anotar os gastos, Lucas percebe que assinaturas pouco usadas consomem R$ 90 por mês. Qual decisão é mais consistente com o objetivo de guardar dinheiro?", "Ajuste de orçamento", "B",
                    "Cancelar ou rever as assinaturas e reservar o valor", "Ignorar os gastos pequenos porque não fazem diferença", "Usar o limite do cartão para cobrir o mês",
                    "Missão cumprida. Pequenos gastos recorrentes podem somar; direcionar o valor liberado para uma meta torna o plano concreto.",
                    "Gastos recorrentes se acumulam. Rever o que tem pouco valor para Lucas pode liberar dinheiro sem cortar o que é importante."),
                q("l1-q6", "Meta do mes", "Lucas decidiu guardar R$ 300 todo mes. Onde esse valor deve aparecer no planejamento dele?", "Meta financeira", "C",
                    "Como compromisso previsto antes dos gastos por impulso", "Apenas se sobrar dinheiro sem nenhum controle", "Como limite extra do cartao de credito",
                    "Boa. Quando a meta entra no orcamento, ela vira decisao planejada, nao sobra acidental.",
                    "Esperar sobrar parece facil, mas costuma falhar. A microdica e tratar a meta como compromisso do mes."),
                q("l1-q7", "Divida cara", "Lucas viu que o rotativo do cartao tem juros altos. Qual atitude reduz o risco de a divida crescer?", "Juros compostos na divida", "A",
                    "Negociar e trocar por uma alternativa mais barata, se necessario", "Ignorar a fatura para pensar nisso depois", "Fazer novas compras para ganhar pontos",
                    "Certo. Divida cara precisa de acao rapida: entender o custo, negociar e evitar novas compras.",
                    "A tentacao e empurrar para depois, mas juros altos trabalham contra Lucas. Primeiro controle o custo da divida."),
                q("l1-q8", "Conexao CPA", "Antes de sugerir qualquer investimento para Lucas, qual informacao basica ainda precisa estar clara?", "Diagnostico financeiro", "B",
                    "Renda, gastos, dividas, reserva e objetivos", "A marca do banco que ele prefere", "O investimento que rendeu mais ontem",
                    "Exato. No atendimento financeiro, recomendacao adequada nasce do diagnostico do cliente.",
                    "Produto vem depois do contexto. A microdica e sempre perguntar: para quem, para quando e com qual risco.")),
            level(2, "Antes de investir", "🛟", "coral",
                "Prepare uma base: objetivos, prazo, reserva, inflação e acesso ao dinheiro.",
                "Missão: a viagem e a reserva de Marina",
                "Marina está guardando dinheiro para uma viagem daqui a dois meses e também quer montar uma reserva para imprevistos.",
                "Marina separou dinheiro de curto prazo da reserva e percebeu que liquidez, prazo e inflacao mudam a escolha. Essa e a base para analisar adequacao antes de comparar rentabilidade.",
                q("l2-q1", "Objetivo e prazo", "Marina separou R$ 300 para uma viagem daqui a dois meses. O que ela deve considerar antes de escolher onde guardar?", "Objetivo e horizonte", "B",
                    "O prazo curto e a data em que vai precisar do dinheiro", "Apenas a maior rentabilidade anunciada", "Somente o nome mais conhecido do produto",
                    "Certo. O prazo do objetivo orienta quanto risco faz sentido e quando o dinheiro precisa estar disponível.",
                    "A maior taxa não basta. Para um objetivo próximo, a data de uso e a possibilidade de resgatar são essenciais."),
                q("l2-q2", "Reserva de emergência", "Marina começa a formar uma reserva para despesas inesperadas. Qual característica costuma ser importante para esse dinheiro?", "Liquidez", "C",
                    "Poder acessar o recurso quando surgir uma emergência", "Ficar bloqueado por vários anos em troca de uma taxa maior", "Variar muito de preço todos os dias",
                    "Exato. Liquidez é a facilidade e o prazo para transformar um investimento em dinheiro disponível.",
                    "Uma reserva pode ser necessária sem aviso. Por isso, liquidez e estabilidade são relevantes; carência longa pode não combinar com essa finalidade."),
                q("l2-q3", "Inflação", "Os preços sobem ao longo do ano. Se o dinheiro de Marina rende menos que essa alta, o que pode ocorrer?", "Poder de compra", "A",
                    "O saldo nominal cresce, mas compra menos coisas", "O poder de compra sempre aumenta", "A inflação cancela automaticamente os juros",
                    "Isso. O rendimento nominal não conta a história toda: é preciso considerar a variação dos preços para entender o ganho real.",
                    "O saldo pode aumentar em reais e ainda assim perder poder de compra se os preços subirem mais."),
                q("l2-q4", "Reserva adequada ao objetivo", "Marina tem R$ 10.000 reservados para emergências e analisa um produto com carência de dois anos. Qual ponto merece atenção primeiro?", "Liquidez e carência", "B",
                    "Se conseguirá acessar o dinheiro antes do fim da carência", "Se o produto tem o maior nome da prateleira", "Se a carência torna o investimento livre de riscos",
                    "Boa leitura. Uma carência longa pode conflitar com a finalidade de uma reserva que talvez precise ser usada rapidamente.",
                    "Carência não significa ausência de risco nem combina automaticamente com qualquer objetivo. Para a reserva, disponibilidade é central."),
                q("l2-q5", "Plano para começar", "Marina ainda não tem reserva e quer começar a investir para uma meta de longo prazo. Qual abordagem é mais cuidadosa?", "Planejamento financeiro", "C",
                    "Definir prioridades, criar uma reserva acessível e planejar a meta", "Aplicar tudo em um produto difícil de resgatar", "Escolher um investimento só pela rentabilidade passada",
                    "Missão concluída. Separar objetivos por prazo ajuda a não depender de um investimento de longo prazo numa emergência.",
                    "Uma estratégia começa pelas necessidades e pelos prazos. Rentabilidade passada não garante resultados futuros e não substitui uma reserva."),
                q("l2-q6", "Liquidez diaria", "Marina compara dois produtos: um rende mais, mas so permite resgate no vencimento. O que deve pesar para a reserva?", "Liquidez versus retorno", "A",
                    "A reserva precisa estar acessivel quando o imprevisto acontecer", "Rentabilidade maior sempre resolve qualquer risco", "Produto com vencimento longo serve para qualquer objetivo",
                    "Isso. Reserva combina mais com acesso e previsibilidade do que com perseguir a maior taxa.",
                    "A taxa chama atencao, mas emergencia tem data incerta. Primeiro vem liquidez."),
                q("l2-q7", "Ganho real", "Se uma aplicacao rende 8% no ano e a inflacao fica em 6%, o que Marina deve perceber?", "Rentabilidade real", "B",
                    "O ganho real aproximado e menor que o ganho nominal", "O ganho real e sempre igual ao rendimento anunciado", "A inflacao nao interfere no poder de compra",
                    "Correto. Rentabilidade real olha o que sobra depois da perda de poder de compra.",
                    "O numero anunciado e nominal. A microdica e sempre perguntar: isso supera a inflacao?"),
                q("l2-q8", "Conexao CPA", "Ao avaliar o objetivo de Marina, qual trio resume melhor uma analise inicial adequada?", "Objetivo, prazo e liquidez", "C",
                    "Objetivo, horizonte de tempo e necessidade de resgate", "Nome do produto, propaganda e popularidade", "Rentabilidade passada, boato e pressa",
                    "Boa. Essa triade aparece o tempo todo em suitability e planejamento financeiro.",
                    "Popularidade nao substitui adequacao. Comece por objetivo, prazo e liquidez.")),
            level(3, "Começando a investir", "🌱", "butter",
                "Conheça renda fixa, títulos públicos, CDBs, fundos e renda variável.",
                "Missão: escolher sem cair no nome bonito",
                "Depois de organizar a reserva, Pedro compara produtos de investimento. Cada escolha precisa combinar com o funcionamento e o prazo do produto.",
                "Pedro saiu do nome do produto e passou a olhar emissor, regra de remuneracao, cotas, prazo e risco. E esse olhar que prepara para a linguagem de investimentos da CPA.",
                q("l3-q1", "Renda fixa", "Ao ouvir que um investimento é de renda fixa, qual interpretação é mais adequada?", "Renda fixa", "C",
                    "As regras de remuneração são definidas no início, mas o resultado pode depender das condições do produto", "O investimento não tem risco e sempre rende o mesmo valor", "O investidor recebe participação nos lucros de uma empresa",
                    "Correto. Renda fixa descreve regras de remuneração; não é sinônimo de retorno garantido ou ausência de risco.",
                    "Renda fixa não quer dizer risco zero. Prazo, emissor, indexador e condições de resgate ainda importam."),
                q("l3-q2", "CDB", "Um CDB é emitido por qual tipo de instituição?", "Certificado de Depósito Bancário", "A",
                    "Um banco ou outra instituição financeira emissora", "O Tesouro Nacional em nome do governo federal", "Uma bolsa de valores em nome do investidor",
                    "Isso. No CDB, o investidor empresta recursos à instituição emissora conforme as condições contratadas.",
                    "CDB significa Certificado de Depósito Bancário: é emitido por uma instituição financeira, não pelo Tesouro ou pela bolsa."),
                q("l3-q3", "Tesouro Direto", "Ao comprar um título público pelo Tesouro Direto, a quem o investidor está emprestando recursos?", "Títulos públicos", "B",
                    "Ao governo federal, conforme as regras do título", "A uma empresa privada escolhida pela corretora", "A outros investidores sem intermediação",
                    "Certo. Títulos públicos são emitidos pelo Tesouro Nacional; cada título tem regras próprias de remuneração e vencimento.",
                    "No Tesouro Direto, o título é público e emitido pelo Tesouro Nacional. Ainda é preciso entender prazo e oscilação em resgates antecipados."),
                q("l3-q4", "Fundos de investimento", "O que representa uma cota de um fundo de investimento?", "Fundos e cotas", "C",
                    "Uma fração do patrimônio do fundo", "Uma promessa de rentabilidade fixa do administrador", "Um depósito bancário automaticamente coberto pelo FGC",
                    "Exato. Ao investir, a pessoa adquire cotas, e o resultado depende dos ativos e das regras do fundo.",
                    "Cota é uma fração do patrimônio do fundo. Fundo não é depósito bancário e não oferece garantia de rentabilidade por definição."),
                q("l3-q5", "Renda variável", "Uma ação pode subir ou cair de preço. O que isso indica para quem investe?", "Renda variável", "A",
                    "O retorno pode variar e há possibilidade de perdas", "O preço só muda quando a empresa distribui dividendos", "A ação tem vencimento e devolve sempre o valor aplicado",
                    "Missão concluída. Na renda variável, preços e resultados podem oscilar; entender isso vem antes de investir.",
                    "O preço das ações varia no mercado e não há devolução garantida do valor investido. Oscilação e perdas fazem parte dos riscos."),
                q("l3-q6", "Marcacao a mercado", "Pedro ouviu que um titulo publico pode oscilar antes do vencimento. O que isso significa?", "Marcacao a mercado", "C",
                    "O preco pode variar se ele vender antes do vencimento", "O governo cancela automaticamente o titulo", "Todo titulo publico perde dinheiro no vencimento",
                    "Certo. Antes do vencimento, o preco pode oscilar; no vencimento valem as regras do titulo.",
                    "Oscilacao nao e cancelamento. A microdica e separar venda antecipada de carregamento ate o vencimento."),
                q("l3-q7", "Custos do fundo", "Um fundo cobra taxa de administracao. Como Pedro deve interpretar esse custo?", "Taxas em fundos", "A",
                    "Como custo que reduz a rentabilidade liquida do cotista", "Como garantia de rendimento maior", "Como imposto devolvido automaticamente",
                    "Exato. Custos importam porque o investidor recebe o resultado liquido.",
                    "Taxa nao garante retorno. Leia custos junto com politica, risco e prazo de resgate."),
                q("l3-q8", "FGC", "Pedro acha que todo produto de investimento tem FGC. Qual leitura e mais correta?", "Garantias e limites", "B",
                    "A cobertura depende do tipo de produto e dos limites vigentes", "Todo fundo de investimento e coberto integralmente", "Acoes tem devolucao garantida pelo FGC",
                    "Boa. Garantia tem regra, elegibilidade e limite; nao vale para tudo.",
                    "A sigla e familiar, mas nao universal. A microdica e checar produto elegivel e limite.")),
            level(4, "Pensando como investidor", "🧭", "ink",
                "Conecte perfil, risco, retorno, diversificação e horizonte de investimento.",
                "Missão: o plano de investimento de Bia",
                "Bia tem objetivos com prazos diferentes e está conhecendo alternativas. Seu desafio é avaliar adequação, não perseguir a maior taxa isoladamente.",
                "Bia aprendeu que risco, retorno, diversificacao, perfil e horizonte precisam conversar entre si. A boa decisao nao e a mais chamativa: e a que combina com a pessoa e com o prazo.",
                q("l4-q1", "Risco e retorno", "Uma oferta promete retorno potencial maior, mas pode oscilar bastante. O que Bia deve entender?", "Relação entre risco e retorno", "A",
                    "Maior retorno potencial pode vir acompanhado de maior risco", "O retorno maior elimina a chance de perda", "O risco importa apenas depois do vencimento",
                    "Certo. Potencial de retorno precisa ser analisado junto com os riscos e a capacidade de suportar perdas.",
                    "Uma possibilidade de retorno maior não elimina riscos. Avaliar ambos é parte da decisão, antes e durante o investimento."),
                q("l4-q2", "Diversificação", "Por que distribuir os investimentos entre alternativas diferentes pode ajudar?", "Diversificação", "B",
                    "Pode reduzir a concentração em uma única fonte de risco", "Garante lucro em qualquer cenário", "Torna desnecessário conhecer os produtos",
                    "Isso. Diversificar pode reduzir concentração, embora não elimine todos os riscos nem garanta lucro.",
                    "Diversificação distribui exposições, mas não torna uma carteira livre de risco ou de perdas."),
                q("l4-q3", "Perfil do investidor", "O que uma análise de perfil procura compreender sobre a pessoa investidora?", "Perfil e tolerância a risco", "C",
                    "Objetivos, situação, experiência e tolerância a riscos", "Apenas o produto com maior rentabilidade recente", "Somente a idade, sem considerar os objetivos",
                    "Correto. Perfil é uma das informações que ajuda a avaliar se um produto faz sentido para aquela pessoa.",
                    "A adequação depende de mais do que idade ou retorno recente: objetivos, conhecimento e tolerância a riscos também contam."),
                q("l4-q4", "Horizonte de investimento", "Bia precisará pagar um curso daqui a seis meses. Por que o prazo deve influenciar a escolha?", "Horizonte e adequação", "A",
                    "Porque ela pode não ter tempo para esperar uma recuperação após oscilações", "Porque um prazo curto garante retorno alto", "Porque o vencimento nunca importa em investimentos",
                    "Boa. O horizonte afeta a capacidade de lidar com oscilações e a compatibilidade com o momento do gasto.",
                    "Um objetivo próximo reduz o tempo para lidar com oscilações ou esperar um vencimento. Prazo não garante retorno."),
                q("l4-q5", "Decisão adequada", "Bia tem baixa tolerância a oscilações e precisará do dinheiro em breve. Uma ação volátil parece alinhada a esse objetivo?", "Adequação entre produto e objetivo", "B",
                    "Não necessariamente; prazo e tolerância precisam ser considerados", "Sim, porque ações sempre recuperam perdas antes do objetivo", "Sim, porque a maior variação garante maior retorno",
                    "Missão cumprida. Uma recomendação educacional começa por conhecer o objetivo e os riscos que a pessoa aceita.",
                    "Não há garantia de recuperação antes da data necessária. Uma alternativa volátil pode não combinar com prazo curto e baixa tolerância."),
                q("l4-q6", "Capacidade de perda", "Bia aceita risco no questionario, mas precisara do dinheiro se perder o emprego. O que isso muda?", "Capacidade versus tolerancia", "B",
                    "A capacidade financeira de assumir risco tambem precisa ser avaliada", "A tolerancia declarada resolve tudo sozinha", "Risco so importa quando o produto e estrangeiro",
                    "Correto. Suitability olha vontade, conhecimento, situacao financeira e objetivo.",
                    "Perfil nao e uma palavra solta. A microdica e cruzar tolerancia com capacidade real de perda."),
                q("l4-q7", "Concentracao", "Bia quer colocar todo o dinheiro em um unico ativo porque ele subiu muito. Qual risco aparece?", "Risco de concentracao", "C",
                    "Ficar dependente demais do desempenho de uma unica alternativa", "Garantir retorno maior por ter conviccao", "Eliminar risco por escolher algo conhecido",
                    "Isso. Concentracao pode ampliar perdas se aquele ativo for mal.",
                    "Alta recente seduz, mas nao elimina risco. Pense em diversificacao e objetivo."),
                q("l4-q8", "Rebalanceamento", "Depois de meses, a carteira de Bia ficou mais arriscada do que o planejado. O que pode fazer sentido?", "Acompanhamento da carteira", "A",
                    "Reavaliar e rebalancear conforme objetivo e perfil", "Nunca revisar a carteira depois da primeira compra", "Comprar mais risco sem olhar o plano",
                    "Boa. Planejamento tambem exige acompanhamento e ajustes.",
                    "Carteira muda com o mercado. A microdica e comparar a carteira atual com o plano original.")),
            level(5, "Desafio CPA", "🎓", "coral",
                "Aplique os conceitos em casos integrados com linguagem mais técnica.",
                "Missão: você é assessor por cinco minutos",
                "Rafael tem R$ 10.000 para uma reserva e um objetivo de longo prazo. Ele demonstra baixa tolerância a perdas e pergunta sobre CDBs, fundos e títulos públicos.",
                "Rafael integrou o caso final: separou reserva e longo prazo, checou liquidez, risco de credito, custos, documentos do fundo e suitability. Essa e a logica central para chegar mais forte ao CPA.",
                q("l5-q1", "Caso integrado · necessidade de liquidez", "Um CDB oferece taxa atraente, mas tem carência de dois anos. Considerando que parte dos recursos é a reserva de Rafael, qual análise é prioritária?", "Liquidez e adequação", "B",
                    "Verificar se a restrição de resgate é compatível com a finalidade de reserva", "Assumir que a taxa maior torna o produto adequado", "Ignorar a carência se o emissor for conhecido",
                    "Correto. A finalidade dos recursos e a disponibilidade do produto devem ser compatíveis antes de comparar taxas.",
                    "Taxa não substitui análise de adequação. Uma reserva pode ser necessária antes do fim da carência."),
                q("l5-q2", "Caso integrado · emissor e garantia", "Ao avaliar um CDB, qual afirmação sobre o emissor e a proteção é mais precisa?", "Risco de crédito e cobertura do FGC", "C",
                    "É uma obrigação da instituição emissora; eventual cobertura depende das regras e limites vigentes do FGC", "É uma obrigação do governo federal sem risco de crédito", "Todo investimento financeiro tem cobertura integral do FGC",
                    "Isso. O risco de crédito está ligado ao emissor. A cobertura do FGC se aplica somente a produtos elegíveis e conforme regras e limites vigentes.",
                    "CDB é obrigação do emissor, não do governo. O FGC não cobre todos os investimentos nem significa cobertura ilimitada."),
                q("l5-q3", "Caso integrado · fundo de investimento", "Rafael avalia um fundo para diversificar. Qual informação ajuda a entender os riscos e custos antes de investir?", "Leitura das características do fundo", "A",
                    "Política de investimento, riscos, taxas, prazo de resgate e documentos do fundo", "Somente o retorno do último mês", "Apenas a quantidade de cotistas",
                    "Boa análise. Política, riscos, custos e condições de resgate ajudam a avaliar se o fundo combina com o objetivo.",
                    "Um retorno recente isolado não explica estratégia, risco, custos nem prazo de resgate. É preciso analisar as características do fundo."),
                q("l5-q4", "Caso integrado · perfil e recomendação", "Rafael declara baixa tolerância a perdas, mas recebe uma sugestão de produto volátil para sua reserva. Qual conduta é mais adequada numa análise de suitability?", "Adequação ao perfil", "B",
                    "Reavaliar objetivo, prazo e perfil antes de considerar a recomendação adequada", "Recomendar mesmo assim, pois a rentabilidade potencial é maior", "Considerar apenas a idade e desconsiderar as respostas de perfil",
                    "Correto. Suitability relaciona características do produto com objetivos, situação e perfil do cliente.",
                    "Potencial de retorno não substitui a avaliação de adequação. Objetivo, horizonte, conhecimento e tolerância ao risco precisam ser considerados."),
                q("l5-q5", "Caso integrado · decisão final", "Rafael separa a reserva de emergência do objetivo de longo prazo. Qual princípio deve orientar a comparação final dos produtos?", "Adequação, risco e horizonte", "C",
                    "Comparar risco, liquidez, custos e prazo com a finalidade de cada parcela", "Escolher uma única alternativa pela maior taxa divulgada", "Tratar todos os produtos como equivalentes se forem investimentos",
                    "Missão CPA concluída. Você conectou objetivo, prazo, risco, liquidez e características do produto numa decisão contextual.",
                    "A taxa isolada não mostra se o produto atende cada objetivo. Compare liquidez, riscos, custos e horizonte para cada parcela."),
                q("l5-q6", "Caso CPA - suitability", "Rafael omite informacoes sobre renda e patrimonio, mas pede recomendacao imediata. Qual postura e mais adequada?", "Suitability e coleta de informacoes", "A",
                    "Explicar que a avaliacao de adequacao depende dessas informacoes", "Recomendar o produto de maior comissao", "Ignorar o perfil porque ele parece decidido",
                    "Correto. Sem informacoes suficientes, a recomendacao perde base de adequacao.",
                    "Pressa nao substitui dever de conhecer o cliente. A microdica e documentar e solicitar dados essenciais."),
                q("l5-q7", "Caso CPA - risco de mercado", "Um titulo prefixado longo pode cair de preco se os juros de mercado subirem. Que risco Rafael deve entender?", "Risco de mercado", "B",
                    "Risco de oscilacao de preco antes do vencimento", "Risco de o FGC deixar de cobrir acoes", "Risco de liquidez diaria obrigatoria",
                    "Exato. A marcacao a mercado afeta o preco de venda antes do vencimento.",
                    "O ponto aqui nao e garantia, e preco. A microdica e relacionar juros, prazo e oscilacao."),
                q("l5-q8", "Caso CPA - fundo", "Rafael escolhe um fundo olhando apenas a rentabilidade passada. Qual alerta tecnico e necessario?", "Rentabilidade passada", "C",
                    "Rentabilidade passada nao garante resultado futuro", "Resultado passado elimina risco de mercado", "Historico dispensa leitura dos documentos",
                    "Certo. Historico ajuda na analise, mas nao substitui risco, politica, custos e prazo.",
                    "Historico seduz porque parece prova. A microdica e tratar passado como dado, nao promessa."),
                q("l5-q9", "Caso CPA - liquidez", "Um fundo tem prazo D+30 para resgate. Para a reserva de emergencia de Rafael, qual problema pode surgir?", "Prazo de cotizacao e liquidacao", "A",
                    "O dinheiro pode nao estar disponivel quando a emergencia ocorrer", "D+30 significa saque instantaneo com bonus", "Prazo de resgate so importa para a corretora",
                    "Boa. Reserva exige compatibilidade entre necessidade de caixa e prazo de resgate.",
                    "D+30 e prazo, nao promessa de dinheiro imediato. A microdica e casar liquidez com finalidade."),
                q("l5-q10", "Caso CPA - credito", "Rafael compara CDBs de bancos diferentes. O que representa risco de credito nesse caso?", "Risco de credito", "B",
                    "A possibilidade de o emissor nao honrar a obrigacao", "A variacao diaria obrigatoria das acoes", "A taxa de administracao de um fundo multimercado",
                    "Correto. Em CDB, o emissor importa; garantia e limite devem ser avaliados junto.",
                    "Aqui o foco e quem deve pagar. A microdica e perguntar: quem e o emissor e qual sua solidez?"),
                q("l5-q11", "Caso CPA - conduta", "Rafael pede uma promessa de rentabilidade para aceitar o investimento. O que o assessor deve evitar?", "Conduta e promessa de retorno", "C",
                    "Prometer retorno futuro como se fosse garantido", "Explicar riscos, custos e cenarios possiveis", "Registrar as informacoes usadas na recomendacao",
                    "Exato. Boa conduta evita promessa indevida e comunica riscos de forma clara.",
                    "Cliente pode pedir certeza, mas investimento envolve risco. A microdica e explicar, nao prometer."),
                q("l5-q12", "Caso CPA - decisao final", "Depois de separar reserva e longo prazo, Rafael aceita mais risco apenas para a parcela de longo prazo. Qual conclusao e mais tecnica?", "Carteira por objetivos", "A",
                    "Cada parcela pode ter produto diferente conforme prazo, liquidez e risco", "Toda carteira deve ter um unico produto para simplificar", "Reserva de emergencia e longo prazo sempre exigem o mesmo risco",
                    "Missao CPA reforcada. A recomendacao adequada segmenta objetivos e compara produtos por risco, liquidez, custo e horizonte.",
                    "Simplificar demais pode prejudicar adequacao. A microdica e dividir a carteira por finalidade."))

        );
    }
}
