package com.gerenciadordearquivos.app

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.SecureRandom
import kotlin.concurrent.thread

/*
 * =============================================================
 * SERVIDOR WI-FI
 * Mini site no celular para o computador (no mesmo Wi-Fi)
 * baixar e enviar arquivos pelo navegador. Só funciona com o
 * código secreto que aparece na tela, e só enquanto a tela de
 * transferência estiver aberta.
 * =============================================================
 */

class ServidorWifi(
    private val raiz: File,
    private val pastaOculta: String,
    private val aoReceber: (String) -> Unit
) {

    val codigo: String =
        (1..6).map { "abcdefghjkmnpqrstuvwxyz23456789"[SecureRandom().nextInt(31)] }
            .joinToString("")

    private var socket: ServerSocket? = null

    var porta: Int = 0
        private set

    fun iniciar(): Boolean {

        for (tentativa in 8080..8090) {
            try {
                socket = ServerSocket(tentativa)
                porta = tentativa
                break
            } catch (_: Exception) {
            }
        }

        val servidor = socket ?: return false

        thread(isDaemon = true) {
            while (!servidor.isClosed) {
                try {
                    val cliente = servidor.accept()
                    thread(isDaemon = true) { atender(cliente) }
                } catch (_: Exception) {
                }
            }
        }

        return true
    }

    fun parar() {
        try { socket?.close() } catch (_: Exception) { }
    }

    // ------------------------------------------------------------

    private fun atender(cliente: Socket) {

        cliente.use { s ->

            try {

                s.soTimeout = 60_000

                val entrada = BufferedInputStream(s.getInputStream(), 64 * 1024)
                val saida = BufferedOutputStream(s.getOutputStream(), 64 * 1024)

                val primeira = lerLinha(entrada) ?: return
                val partes = primeira.split(" ")
                if (partes.size < 2) return

                val metodo = partes[0]
                val alvo = partes[1]

                val cabecalhos = HashMap<String, String>()
                while (true) {
                    val linha = lerLinha(entrada) ?: break
                    if (linha.isEmpty()) break
                    val i = linha.indexOf(':')
                    if (i > 0) cabecalhos[linha.substring(0, i).trim().lowercase()] = linha.substring(i + 1).trim()
                }

                val caminho = alvo.substringBefore('?')
                val parametros = lerParametros(alvo.substringAfter('?', ""))

                // Tudo precisa começar com /código
                if (!caminho.startsWith("/$codigo")) {
                    responderTexto(saida, 403, tr("Código inválido. Use o endereço mostrado no celular."))
                    return
                }

                val rota = caminho.removePrefix("/$codigo").trimStart('/')

                val pasta = resolver(parametros["p"] ?: "")

                when {
                    metodo == "GET" && rota == "baixar" ->
                        baixar(saida, pasta)

                    metodo == "POST" && rota == "enviar" ->
                        receber(entrada, saida, cabecalhos, pasta)

                    metodo == "GET" ->
                        listar(saida, pasta)

                    else ->
                        responderTexto(saida, 405, tr("Método não suportado"))
                }

                saida.flush()

            } catch (_: Exception) {
            }
        }
    }

    private fun lerLinha(entrada: InputStream): String? {

        val bytes = java.io.ByteArrayOutputStream()

        while (true) {
            val b = entrada.read()
            if (b < 0) return if (bytes.size() == 0) null else bytes.toString("UTF-8")
            if (b == '\n'.code) break
            if (b != '\r'.code) bytes.write(b)
            if (bytes.size() > 16 * 1024) return null
        }

        return bytes.toString("UTF-8")
    }

    private fun lerParametros(texto: String): Map<String, String> =
        texto.split('&')
            .filter { it.contains('=') }
            .associate {
                URLDecoder.decode(it.substringBefore('='), "UTF-8") to
                    URLDecoder.decode(it.substringAfter('='), "UTF-8")
            }

    // Caminho relativo -> arquivo dentro da raiz (nunca fora dela)
    private fun resolver(relativo: String): File? {

        val arquivo = File(raiz, relativo).canonicalFile
        val base = raiz.canonicalFile

        val dentro =
            arquivo == base ||
                arquivo.path.startsWith(base.path + File.separator)

        if (!dentro) return null

        if (arquivo.path.split(File.separator).contains(pastaOculta)) return null

        return arquivo
    }

    private fun relativo(arquivo: File): String =
        arquivo.canonicalPath
            .removePrefix(raiz.canonicalPath)
            .trimStart(File.separatorChar)

    private fun url(rota: String, arquivo: File): String =
        "/$codigo/$rota?p=" + URLEncoder.encode(relativo(arquivo), "UTF-8")

    private fun escapar(texto: String): String =
        texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun tamanho(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024)
        return when {
            mb >= 1024 -> String.format(java.util.Locale.US, "%.2f GB", mb / 1024)
            mb >= 1 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            else -> "${bytes / 1024} KB"
        }
    }

    // ------------------------------------------------------------

    private fun listar(saida: OutputStream, pasta: File?) {

        if (pasta == null || !pasta.isDirectory) {
            responderTexto(saida, 404, tr("Pasta não encontrada"))
            return
        }

        val itens =
            (pasta.listFiles() ?: emptyArray())
                .filter { !it.name.startsWith(".") }
                .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })

        val html = StringBuilder()

        html.append(
            """<!doctype html><html lang="pt-BR"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Arquivos Pro - ${escapar(pasta.name.ifEmpty { "Celular" })}</title>
<style>
body{font-family:system-ui,sans-serif;margin:0;background:#f5f5f5;color:#222}
header{background:#242424;color:#fff;padding:14px 20px;font-size:20px;font-weight:bold}
main{max-width:900px;margin:0 auto;padding:16px}
.caixa{background:#fff;border-radius:12px;padding:14px;margin-bottom:14px}
a{color:#1565c0;text-decoration:none}
li{padding:8px 0;border-bottom:1px solid #eee;list-style:none;display:flex;justify-content:space-between;gap:12px}
ul{padding:0;margin:0}
.t{color:#777;white-space:nowrap}
button{background:#1e88e5;color:#fff;border:0;border-radius:20px;padding:10px 18px;font-size:15px;cursor:pointer}
</style></head><body>
<header>🧹 Arquivos Pro — arquivos do celular</header><main>
<div class="caixa"><b>📁 /${escapar(relativo(pasta))}</b>"""
        )

        if (pasta.canonicalFile != raiz.canonicalFile) {
            pasta.parentFile?.let {
                html.append(""" &nbsp; <a href="${url("", it)}">⬆ Voltar</a>""")
            }
        }

        html.append(
            """</div>
<div class="caixa"><form method="post" enctype="multipart/form-data" action="${url("enviar", pasta)}">
<b>Enviar do computador para esta pasta:</b><br><br>
<input type="file" name="arquivos" multiple> <button type="submit">Enviar</button>
</form></div><div class="caixa"><ul>"""
        )

        if (itens.isEmpty()) html.append("<li>Pasta vazia</li>")

        for (item in itens) {
            if (item.isDirectory) {
                html.append("""<li><a href="${url("", item)}">📁 ${escapar(item.name)}</a></li>""")
            } else {
                html.append(
                    """<li><a href="${url("baixar", item)}">📄 ${escapar(item.name)}</a><span class="t">${tamanho(item.length())}</span></li>"""
                )
            }
        }

        html.append("</ul></div></main></body></html>")

        val bytes = html.toString().toByteArray(Charsets.UTF_8)

        saida.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray()
        )

        saida.write(bytes)
    }

    private fun baixar(saida: OutputStream, arquivo: File?) {

        if (arquivo == null || !arquivo.isFile) {
            responderTexto(saida, 404, tr("Arquivo não encontrado"))
            return
        }

        val nome = URLEncoder.encode(arquivo.name, "UTF-8").replace("+", "%20")

        saida.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n" +
                "Content-Length: ${arquivo.length()}\r\n" +
                "Content-Disposition: attachment; filename*=UTF-8''$nome\r\n" +
                "Connection: close\r\n\r\n").toByteArray()
        )

        FileInputStream(arquivo).use { it.copyTo(saida, 64 * 1024) }
    }

    // Recebe arquivos de um formulário multipart/form-data
    private fun receber(
        entrada: InputStream,
        saida: OutputStream,
        cabecalhos: Map<String, String>,
        pasta: File?
    ) {

        if (pasta == null || !pasta.isDirectory) {
            responderTexto(saida, 404, tr("Pasta não encontrada"))
            return
        }

        val tipo = cabecalhos["content-type"] ?: ""
        val limite = tipo.substringAfter("boundary=", "").trim('"')

        if (limite.isEmpty()) {
            responderTexto(saida, 400, tr("Envio inválido"))
            return
        }

        val delimitador = "\r\n--$limite".toByteArray()

        // Pula até o primeiro "--limite"
        lerLinha(entrada)

        var recebidos = 0

        while (true) {

            var nomeArquivo: String? = null

            while (true) {
                val linha = lerLinha(entrada) ?: break
                if (linha.isEmpty()) break
                if (linha.lowercase().startsWith("content-disposition")) {
                    nomeArquivo =
                        Regex("filename=\"([^\"]*)\"").find(linha)?.groupValues?.get(1)
                }
            }

            val nomeLimpo =
                nomeArquivo
                    ?.substringAfterLast('/')
                    ?.substringAfterLast('\\')
                    ?.takeIf { it.isNotBlank() && !it.startsWith(".") }

            val destino =
                nomeLimpo?.let { Armazenamento.nomeLivre(pasta, it) }

            val escritor =
                destino?.let { BufferedOutputStream(FileOutputStream(it), 64 * 1024) }

            val terminou =
                try {
                    copiarAteDelimitador(entrada, escritor, delimitador)
                } finally {
                    escritor?.close()
                }

            if (destino != null) {
                recebidos++
                aoReceber(destino.name)
            }

            // Depois do delimitador: "--" (fim) ou "\r\n" (próxima parte)
            val a = entrada.read()
            val b = entrada.read()

            if (!terminou || a < 0 || (a == '-'.code && b == '-'.code)) break
        }

        // Volta para a pasta
        saida.write(
            ("HTTP/1.1 303 See Other\r\nLocation: ${url("", pasta)}\r\n" +
                "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray()
        )
    }

    // Copia bytes até achar o delimitador (busca KMP).
    // Devolve false se a conexão acabou antes.
    private fun copiarAteDelimitador(
        entrada: InputStream,
        saida: OutputStream?,
        delimitador: ByteArray
    ): Boolean {

        val falha = IntArray(delimitador.size)
        var k = 0
        for (i in 1 until delimitador.size) {
            while (k > 0 && delimitador[i] != delimitador[k]) k = falha[k - 1]
            if (delimitador[i] == delimitador[k]) k++
            falha[i] = k
        }

        var casados = 0

        while (true) {

            val lido = entrada.read()
            if (lido < 0) return false

            val b = lido.toByte()

            while (casados > 0 && b != delimitador[casados]) {
                // Os bytes que estavam "quase casando" eram dados
                val novo = falha[casados - 1]
                saida?.write(delimitador, 0, casados - novo)
                casados = novo
            }

            if (b == delimitador[casados]) {
                casados++
                if (casados == delimitador.size) return true
            } else {
                saida?.write(lido)
            }
        }
    }

    private fun responderTexto(saida: OutputStream, codigoHttp: Int, texto: String) {

        val bytes = texto.toByteArray(Charsets.UTF_8)

        saida.write(
            ("HTTP/1.1 $codigoHttp Erro\r\nContent-Type: text/plain; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray()
        )

        saida.write(bytes)
    }

    companion object {

        // IP do celular na rede Wi-Fi
        fun enderecoLocal(): String? =
            try {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filter { it.isUp && !it.isLoopback }
                    .sortedByDescending { it.name.startsWith("wlan") }
                    .flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { it.isSiteLocalAddress }
                    ?.hostAddress
            } catch (_: Exception) {
                null
            }
    }
}
