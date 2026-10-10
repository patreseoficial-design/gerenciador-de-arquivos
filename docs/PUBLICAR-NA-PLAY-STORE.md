# Como publicar o Gerenciador de Arquivos Pro na Play Store

Siga os passos na ordem. Tudo que for para **copiar e colar** está em blocos.

---

## 1. Chave de assinatura (uma vez só)

A versão da Play Store precisa ser assinada com uma chave sua. Ela já foi gerada e entregue a você em 3 arquivos (`upload.jks`, `KEYSTORE_BASE64.txt` e `LEIA-ME-CHAVE.txt`).

1. **Guarde os 3 arquivos num lugar seguro**, por exemplo no seu Google Drive pessoal. **Nunca** coloque esses arquivos no GitHub.
2. No GitHub, abra o repositório e vá em **Settings → Secrets and variables → Actions → New repository secret**.
3. Crie os 4 segredos com os nomes e valores do `LEIA-ME-CHAVE.txt`:
   - `KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

A partir daí, cada build no GitHub Actions gera também o artefato **`app-release-playstore`**. Esse é o arquivo `.aab` que vai para a Play Store. O número da versão sobe sozinho a cada build.

---

## 2. Conta de desenvolvedor

1. Acesse https://play.google.com/console e crie a conta (taxa única de **US$ 25**).
2. Faça a verificação de identidade que o Google pedir.
3. Contas pessoais novas precisam fazer um **teste fechado** antes de publicar (passo 5).

---

## 3. Criar o app no Play Console

- **Nome do app na loja (título):** `Gerenciador de Arquivos Pro` (27 caracteres). A frase exata "gerenciador de arquivos" no título ajuda a aparecer nessa busca. **"Limpa celular"** fica no começo da descrição curta, para aparecer também nessa busca. O mesmo nome aparece no topo do app e embaixo do ícone (alguns celulares encurtam nomes longos embaixo do ícone). O título pode ser trocado depois a qualquer momento.
- **Idioma padrão:** Português (Brasil)
- **App ou jogo:** App
- **Gratuito**
- Ative a **Assinatura de apps do Google Play** (vem ligada por padrão). Com ela, se você perder a chave, dá para pedir a troca.

### Texto da loja

**Descrição curta** (até 80 caracteres):

```
Limpa celular: WhatsApp, fotos repetidas e arquivos grandes. Fácil e rápido!
```

**Descrição completa:**

```
Celular cheio? O Gerenciador de Arquivos Pro limpa o celular, organiza e protege seus arquivos de um jeito simples e todo em português.

🧹 LIMPEZA DO WHATSAPP
• Veja quanto ocupam fotos, vídeos, áudios, figurinhas e documentos do WhatsApp e do WhatsApp Business
• Marque os arquivos com mais de 30 dias e libere espaço com 1 toque

🖼️ FOTOS DUPLICADAS
• Encontre fotos e vídeos repetidos no celular
• O original fica guardado e as cópias vão embora

📊 ANÁLISE DO ARMAZENAMENTO
• Veja o que está ocupando espaço: fotos, vídeos, músicas, documentos e instaladores
• Sugestões do que apagar: arquivos grandes, downloads antigos, APKs esquecidos e mais

🗑️ LIXEIRA COM RESTAURAR
• Nada some sem querer: restaure o que apagou ou esvazie quando quiser

🔒 COFRE
• Esconda fotos e arquivos pessoais com PIN

🗜️ COMPRIMIR FOTOS
• Deixe as fotos até 4 vezes menores, mantendo a qualidade, a data e o local

💻 PASSAR PARA O COMPUTADOR
• Baixe e envie arquivos entre o celular e o PC pelo Wi-Fi, sem cabo

🔔 AVISO DE CELULAR CHEIO E WIDGET
• Receba um aviso quando o espaço estiver acabando
• Widget com o espaço livre na tela inicial

🔠 LETRAS GRANDES
• Modo com letras maiores, ótimo para quem enxerga pouco

📁 TUDO ORGANIZADO
• Imagens, Vídeos, Áudios, Documentos, Downloads e Aplicativos
• Ícones para cada tipo de arquivo (PDF, Word, Excel, músicas, vídeos, APKs...)
• Fotos em alta qualidade, player de vídeo e música, leitor de PDF e ZIP
• Cartão de memória: veja o espaço e mova arquivos entre o celular e o cartão

⭐ PREMIUM (compra única, sem assinatura)
• Cofre, apagar duplicadas com 1 toque, comprimir fotos e sem anúncios

Seus arquivos nunca saem do seu celular.
```

**Imagens necessárias:**
- Ícone de 512 × 512 px
- Imagem de destaque de 1024 × 500 px
- Pelo menos **2 capturas de tela** do celular. Boas opções: a tela inicial, a Análise com as sugestões, a Lixeira e uma foto aberta.

**Categoria:** Ferramentas
**Política de privacidade (URL):**

```
https://github.com/patreseoficial-design/gerenciador-de-arquivos/blob/main/docs/politica-de-privacidade.md
```


---

## 3.1 Outros idiomas e países

O app já vem traduzido para inglês, espanhol, francês, alemão, italiano e indonésio. Os textos da loja nesses idiomas estão em [LOJA-EM-OUTROS-IDIOMAS.md](LOJA-EM-OUTROS-IDIOMAS.md), junto com o passo a passo para liberar todos os países.

---

## 4. Formulários do "Conteúdo do app"

### Segurança dos dados (com anúncios do AdMob)

Responda assim. Confira também o guia oficial do Google: https://support.google.com/admob/answer/11581186

- O app coleta ou compartilha dados do usuário? → **Sim**
- Os dados são criptografados em trânsito? → **Sim**
- O usuário pode pedir a exclusão dos dados? → **Não** (o app não guarda dados em servidor; os dados do AdMob são do Google)
- Tipos de dados (todos **coletados e compartilhados**, para **Publicidade ou marketing**, **Análise** e **Prevenção de fraudes**):
  - **Identificadores do dispositivo ou outros IDs** (ID de publicidade)
  - **Localização aproximada** (pelo IP)
  - **Atividade no app → Interações com o app**
  - **Informações e desempenho do app → Registros de falhas e Diagnóstico**
- **Compras** (Premium): a Google Play processa o pagamento. O app não coleta dados de pagamento.

### Anúncios

- O app contém anúncios? → **Sim**

### Classificação de conteúdo

- Preencha o questionário: categoria **Utilitário/Ferramentas**, sem violência, sem conteúdo sexual, sem interação entre usuários. O resultado deve ser **Livre**.

### Público-alvo

- Faixa etária: **18 anos ou mais** (ou 13+). Não marque crianças.

### Declaração: Acesso a todos os arquivos (MANAGE_EXTERNAL_STORAGE)

- Tipo de uso: **Gerenciador de arquivos**
- Texto:

```
O app é um gerenciador de arquivos. Sua função principal é permitir que o usuário navegue, abra, copie, mova, renomeie e apague arquivos e pastas em todo o armazenamento do aparelho e no cartão de memória, além de analisar o espaço ocupado e sugerir arquivos para limpeza. Essas funções exigem acesso amplo aos arquivos, que não é possível apenas com o MediaStore ou o Storage Access Framework. O app explica o motivo antes de pedir a permissão e não envia nenhum arquivo para a internet.
```

- **Vídeo:** o Google costuma pedir um vídeo curto (pode ser gravado com a tela do celular) mostrando o app pedindo a permissão e usando as funções: navegar em pastas, mover um arquivo e usar a Análise. Suba no YouTube como "não listado" e cole o link.

### Declaração: REQUEST_DELETE_PACKAGES (se o Console pedir)

```
O app tem uma aba "Aplicativos" onde o usuário pode ver os apps instalados e, se quiser, tocar em "Desinstalar". O app apenas abre a tela de desinstalação do próprio Android, que sempre pede a confirmação do usuário. O app nunca remove aplicativos sozinho.
```

> Se essa declaração for recusada, dá para tirar a opção "Desinstalar" e mandar o usuário para a tela de informações do app (onde já existe o botão Desinstalar do Android). É só pedir.

---

## 5. Teste fechado (obrigatório para contas pessoais novas)

1. Em **Testes → Teste fechado**, crie uma faixa.
2. Envie o arquivo `.aab` (artefato `app-release-playstore` do GitHub Actions).
3. Adicione os e-mails (conta Google) de **pelo menos 12 pessoas** (amigos, família).
4. Mande o link de participação para elas. Elas precisam **instalar e manter o app por 14 dias seguidos**.
5. Peça para usarem de verdade e mandarem opinião. Isso também ajuda a achar erros.

Depois dos 14 dias, o Console libera o pedido de **acesso à produção**.

---

## 6. Publicar

1. Em **Produção**, crie uma versão e envie o `.aab` mais recente.
2. Revise e envie para análise. A primeira análise costuma levar de alguns dias a 1 ou 2 semanas.

---

## 7. Dinheiro: anúncios (AdMob) e Premium (R$ 9,90)

### Onde você recebe (perfil de pagamentos)

Tudo é ligado à sua conta Google (**patreseoficial@gmail.com**):
- **Vendas do Premium:** em **Play Console → Configurar → Perfil de pagamentos**, crie o perfil de comerciante com seus dados e conta bancária. A Google fica com **15%** de cada venda; o resto cai na sua conta todo mês.
- **Anúncios:** em **AdMob → Pagamentos**, cadastre seus dados e conta bancária. O AdMob paga quando o saldo passa do valor mínimo da sua região.

### Criar o produto Premium (depois de enviar o primeiro AAB)

O Play Console só deixa criar produtos depois que um AAB com a biblioteca de compras foi enviado (o teste fechado do passo 5 já serve).

1. **Monetizar → Produtos → Produtos no app → Criar produto**
2. **ID do produto:** `premium` (exatamente assim, tudo minúsculo)
3. **Nome:** Gerenciador de Arquivos Pro Premium
4. **Descrição:** Cofre com PIN, apagar fotos duplicadas e sem anúncios.
5. **Preço:** R$ 9,90 → **Ativar**

Para testar sem pagar: em **Configurações → Teste de licença**, adicione o seu e-mail. Compras feitas com ele são de teste.

### Ligar os anúncios de verdade (AdMob)

Os IDs reais da conta AdMob já estão no `app/build.gradle.kts`:

- ID do app: `ca-app-pub-9393930095097786~3410674038`
- Banner (tela inicial): `ca-app-pub-9393930095097786/8494645609`
- Intersticial (depois das limpezas): `ca-app-pub-9393930095097786/8223182860`

A **versão da Play Store** (`app-release-playstore`) usa esses anúncios de verdade. A **versão de teste** (`app-debug`) continua com os anúncios de teste do Google.

Para trocar algum ID sem mexer no código, crie os Secrets `ADMOB_APP_ID`, `ADMOB_BANNER_ID` ou `ADMOB_INTERSTICIAL_ID` no GitHub. Eles têm prioridade.

Ainda no AdMob:
1. Em **Privacidade e mensagens**, crie a mensagem de consentimento (GDPR) para a Europa. O app já mostra essa mensagem quando for necessário.
2. Depois de publicado, ligue o app à Play Store (**Apps → Configurações do app**).

> ⚠️ Nunca clique nos seus próprios anúncios de verdade: o AdMob bloqueia a conta. Para testar, use a versão de teste (`app-debug`) ou adicione seu celular como "dispositivo de teste" no AdMob.

### Como os anúncios aparecem (para não irritar o usuário)

- Um banner no fim da tela inicial.
- Uma tela cheia **só depois de uma limpeza concluída**, no máximo a cada 3 minutos.
- Quem compra o Premium não vê nenhum anúncio.

## 8. Antes de cada atualização

- Teste a versão de teste (`app-debug`) no seu celular.
- Responda às avaliações na Play Store: isso melhora a nota e a confiança.
