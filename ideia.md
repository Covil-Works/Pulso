Especificação Técnica e Funcional: App de Controle de Pressão Arterial

1. Visão Geral do Projeto

Aplicativo móvel focado no registro, acompanhamento e geração de relatórios de pressão arterial. O aplicativo é projetado para funcionar de forma 100% offline, mantendo os dados seguros no dispositivo do usuário e focando em uma experiência simples de três telas principais.

1.1. Stack Tecnológico

Linguagem: Kotlin

Interface: Jetpack Compose

Banco de Dados Local: Room Database

Injeção de Dependência: Dagger Hilt

Padrão de Apresentação: MVVM (Model-View-ViewModel)

2. Arquitetura do Sistema

A arquitetura do projeto seguirá a abordagem Package by Feature (Pacote por Funcionalidade), subdividido internamente por Package by Layer (UI, Domain, Data) para garantir o isolamento, escalabilidade e facilidade de manutenção.

2.1. Estrutura de Diretórios (Exemplo)

com.app.pressao
├── core                  # Configurações globais (Hilt, UI Theme, extensões)
├── feature_perfil        # Onboarding e edição de dados do usuário
├── feature_registro      # Tela principal, calendário, bottom sheet de adição
│   ├── ui                # Telas (Compose), ViewModels, Componentes visuais
│   ├── domain            # Modelos de negócio, Casos de Uso (UseCases), Interfaces de Repositório
│   └── data              # Entities do Room, DAOs, Implementação dos Repositórios
├── feature_metas         # Configuração de dias, horários e notificações
└── feature_relatorio     # Geração e exportação do PDF


2.2. Fluxo de Dados (MVVM + Clean Architecture simplificada)

UI (Compose): Observa os estados emitidos pela ViewModel. Captura eventos do usuário (ex: clique no botão de salvar).

ViewModel: Recebe eventos da UI, executa a lógica de apresentação e chama os Use Cases (Domain).

Domain: Contém as regras de negócio puras (ex: validação se a pressão está entre 0 e 300, cálculo de média).

Data (Room): Executa as operações de leitura/escrita no banco de dados local.

3. Estrutura de Telas e Funcionalidades

O aplicativo possui um fluxo inicial (Onboarding) e três telas principais acessíveis via navegação inferior (Bottom Navigation).

3.1. Onboarding e Perfil

Primeiro Acesso: Ao abrir o app pela primeira vez, uma tela solicitará o Nome e a Idade do usuário.

Edição: Na Tela Principal, haverá um menu superior (três pontinhos) com a opção "Perfil", permitindo atualizar esses dados.

Uso: Esses dados são armazenados no Room e usados exclusivamente para compor o cabeçalho do PDF no Relatório.

3.2. Tela 1: Principal (Dashboard)

Central de acompanhamento diário e inserção de dados.

Calendário: Exibe apenas o mês atual em um contêiner no topo. Dias que possuem registros de medição ficarão destacados (ex: com um círculo ao redor do número). Clicar no mês/setas permite navegar para meses anteriores.

Métricas Resumidas (Abaixo do calendário):

Pressão Média: Exibe a média aritmética das pressões (sistólica e diastólica) registradas no período.

Sequência (Streak): Barra ou indicador textual mostrando a consistência (ex: "Você está medindo há X dias seguidos").

Botão de Adição (FAB): Um botão flutuante com ícone de "+" que aciona um Bottom Sheet.

Regras do Bottom Sheet de Registro:

Layout: Dois campos de texto lado a lado.

Esquerda: Pressão Sistólica (Maior).

Direita: Pressão Diastólica (Menor).

Validação: Apenas números inteiros, maiores que 0 e menores que 300.

Observações: Um Checkbox "Adicionar observação". Se marcado, expande um campo de texto (ex: "estava me sentindo mal hoje").

Feedback Visual ao Salvar: O sistema avalia a entrada e gera um registro com cor correspondente:

Verde (Ótima): < 120 / 80 mmHg.

Amarelo (Razoável): 120-139 / 80-89 mmHg.

Vermelho (Risco): ≥ 140 / 90 mmHg.

3.3. Tela 2: Metas (Alarmes e Hábitos)

Focada em criar a rotina de medição por meio de notificações locais.

Seleção de Dias: Usuário define em quais dias da semana pretende registrar a pressão (ex: Segunda, Quarta, Sexta).

Frequência Diária: Quantas vezes por dia e definição dos horários exatos para a medição.

Notificações (Push): Com base nos horários definidos, o sistema dispara um alarme/notificação push local: "Hora de medir sua pressão".

Progresso de Metas: Seção inferior que cruza a meta com as medições reais, exibindo mensagens de incentivo como: "Você está registrando há X dias! Não falhou nenhum dia!" ou indicando dias esquecidos.

3.4. Tela 3: Relatórios (Geração de PDF)

Tela focada na exportação de dados para o médico.

Geração: Um botão central de "Gerar Relatório em PDF".

Estrutura do PDF (Fixo e Padrão):

Cabeçalho: Nome e Idade do paciente (buscados do Perfil).

Resumo Estatístico:

Pico Máximo: Registro com a pressão mais alta no período e a data da ocorrência.

Pico Mínimo: Registro com a pressão mais baixa no período e a data da ocorrência.

Pressão Média: Média geral do período selecionado.

Histórico Detalhado: Lista sequencial (tabela ou lista estruturada) com: Data, Hora, Pressão (Sistólica/Diastólica), Cor de Risco e Anotações (caso o usuário tenha inserido algo no checkbox do Bottom Sheet).

4. Banco de Dados (Entidades Room Sugeridas)

UserEntity: id (PK), nome (String), idade (Int).

BloodPressureEntity: id (PK), systolic (Int), diastolic (Int), timestamp (Long), notes (String?), colorCode (Int/Enum).

GoalEntity: id (PK), daysOfWeek (List), timesOfDay (List para horários).