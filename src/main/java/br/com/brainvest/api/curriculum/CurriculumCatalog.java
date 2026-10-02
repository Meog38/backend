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
                level.missionTitle(), level.missionBrief(),
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
                               String missionTitle, String missionBrief, Question... questions) {
        return new Level(id, title, emoji, tone, description, missionTitle, missionBrief, List.of(questions));
    }

    private static List<Level> buildLevels() {
        return List.of(
            level(1, "Minha vida financeira", "🧾", "mint",
                "Entenda para onde seu dinheiro vai e tome decisões melhores no dia a dia.",
                "Missão: o primeiro salário de Lucas",
                "Lucas tem 19 anos, recebe R$ 2.400 líquidos e gasta R$ 1.700 por mês. Ele quer se organizar sem deixar de aproveitar a vida.",
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
                    "Gastos recorrentes se acumulam. Rever o que tem pouco valor para Lucas pode liberar dinheiro sem cortar o que é importante.")),
            level(2, "Antes de investir", "🛟", "coral",
                "Prepare uma base: objetivos, prazo, reserva, inflação e acesso ao dinheiro.",
                "Missão: a viagem e a reserva de Marina",
                "Marina está guardando dinheiro para uma viagem daqui a dois meses e também quer montar uma reserva para imprevistos.",
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
                    "Uma estratégia começa pelas necessidades e pelos prazos. Rentabilidade passada não garante resultados futuros e não substitui uma reserva.")),
            level(3, "Começando a investir", "🌱", "butter",
                "Conheça renda fixa, títulos públicos, CDBs, fundos e renda variável.",
                "Missão: escolher sem cair no nome bonito",
                "Depois de organizar a reserva, Pedro compara produtos de investimento. Cada escolha precisa combinar com o funcionamento e o prazo do produto.",
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
                    "O preço das ações varia no mercado e não há devolução garantida do valor investido. Oscilação e perdas fazem parte dos riscos.")),
            level(4, "Pensando como investidor", "🧭", "ink",
                "Conecte perfil, risco, retorno, diversificação e horizonte de investimento.",
                "Missão: o plano de investimento de Bia",
                "Bia tem objetivos com prazos diferentes e está conhecendo alternativas. Seu desafio é avaliar adequação, não perseguir a maior taxa isoladamente.",
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
                    "Não há garantia de recuperação antes da data necessária. Uma alternativa volátil pode não combinar com prazo curto e baixa tolerância.")),
            level(5, "Desafio CPA", "🎓", "coral",
                "Aplique os conceitos em casos integrados com linguagem mais técnica.",
                "Missão: você é assessor por cinco minutos",
                "Rafael tem R$ 10.000 para uma reserva e um objetivo de longo prazo. Ele demonstra baixa tolerância a perdas e pergunta sobre CDBs, fundos e títulos públicos.",
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
                    "A taxa isolada não mostra se o produto atende cada objetivo. Compare liquidez, riscos, custos e horizonte para cada parcela."))
        );
    }
}