# Como publicar o Gerenciador de Arquivos na Play Store

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

- **Nome do app:** escolha um nome próprio, não genérico. Exemplos: "Arquivos Fácil", "Organiza Arquivos", "Limpa & Organiza". O nome "Gerenciador de Arquivos" puro some na busca no meio de centenas de apps iguais.
- **Idioma padrão:** Português (Brasil)
- **App ou jogo:** App
- **Gratuito**
- Ative a **Assinatura de apps do Google Play** (vem ligada por padrão). Com ela, se você perder a chave, dá para pedir a troca.

### Texto da loja

**Descrição curta** (até 80 caracteres):

```
Organize, limpe e libere espaço no celular de um jeito simples e em português.
```

**Descrição completa:**

```
Gerenciador de arquivos simples, leve e todo em português, feito para quem quer encontrar e organizar as coisas do celular sem complicação.

LIBERE ESPAÇO
• Análise do armazenamento: veja quanto ocupam fotos, vídeos, músicas, documentos e instaladores
• Sugestões do que apagar: arquivos grandes, downloads antigos, APKs esquecidos, miniaturas em cache e arquivos vazios
• Lixeira com restaurar: nada some sem querer, e você esvazia quando quiser

TUDO ORGANIZADO
• Imagens, Vídeos, Áudios, Documentos e Downloads separados
• Ícones para cada tipo de arquivo (PDF, Word, Excel, músicas, vídeos, APKs...)
• Downloads do mais recente para o mais antigo

VEJA SEM SAIR DO APP
• Fotos em alta qualidade, com zoom
• Player de vídeo: deslize para o lado para ir ao próximo
• Player de música, leitor de PDF e abertura de arquivos ZIP

CARTÃO DE MEMÓRIA
• Veja o espaço do cartão e o que tem nele
• Mova arquivos do celular para o cartão e do cartão para o celular

APLICATIVOS
• Lista separada entre apps do celular e apps baixados
• Abra, veja informações ou desinstale

PRIVACIDADE
• Sem acesso à internet: seus arquivos nunca saem do celular
• Nenhum dado é coletado
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

> Antes de publicar, troque `[SEU E-MAIL DE CONTATO]` no arquivo `docs/politica-de-privacidade.md` pelo seu e-mail.

---

## 4. Formulários do "Conteúdo do app"

### Segurança dos dados

- O app coleta ou compartilha algum dos tipos de dados do usuário? → **Não**
- (O app não tem permissão de internet, não tem anúncios nem analytics.)

### Anúncios

- O app contém anúncios? → **Não** (por enquanto, veja o passo 7)

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

## 7. Plano de ganhos (sugestão)

1. **Lançar sem anúncios** para juntar as primeiras avaliações boas. Nota alta é o que faz o app aparecer na busca.
2. Quando passar de cerca de **1.000 usuários ativos por dia**, adicionar:
   - um **banner discreto** só na tela inicial;
   - no máximo um anúncio de tela cheia **depois de uma limpeza concluída** ("Você liberou X MB!"), nunca no meio do uso.
3. Oferecer uma **versão sem anúncios** com compra única barata.
4. Colocar anúncios exige: SDK do AdMob, permissão de internet, a tela de consentimento (UMP) e atualizar a Segurança dos dados e esta política. Isso é feito numa versão futura.

## 8. Antes de cada atualização

- Teste a versão de teste (`app-debug`) no seu celular.
- Responda às avaliações na Play Store: isso melhora a nota e a confiança.
