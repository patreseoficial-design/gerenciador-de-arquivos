package com.gerenciadordearquivos.app

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast

/*
 * =============================================================
 * PASSAR ARQUIVOS PARA O COMPUTADOR PELO WI-FI
 * Enquanto esta tela estiver aberta, o computador acessa os
 * arquivos do celular pelo navegador (mesma rede Wi-Fi).
 * =============================================================
 */

class TransferenciaWifiActivity : TelaBase() {

    private var servidor: ServidorWifi? = null

    private val recebidos = mutableListOf<String>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = tr("Passar para o computador")

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lista.visibility = View.GONE

        iniciar()
    }

    override fun onDestroy() {

        servidor?.parar()

        super.onDestroy()
    }

    private fun iniciar() {

        val ip = ServidorWifi.enderecoLocal()

        acoes.removeAllViews()

        if (ip == null) {

            status.text = tr("📶 Conecte o celular a uma rede Wi-Fi")

            acoes.addView(
                criarTexto(
                    tr("O celular e o computador precisam estar na mesma rede Wi-Fi (ou o computador conectado no roteador do celular)."),
                    16f,
                    COR_TEXTO
                )
            )

            adicionarBotao(acoes, criarBotao(tr("Tentar de novo"), null, COR_AZUL) { iniciar() })

            return
        }

        val novo =
            ServidorWifi(Armazenamento.raizCelular, ".GerenciadorArquivos") { nome ->
                runOnUiThread {
                    recebidos.add(0, nome)
                    mostrarRecebidos()
                }
            }

        if (!novo.iniciar()) {
            status.text = tr("Não foi possível iniciar a transferência")
            return
        }

        servidor = novo

        val endereco = "http://$ip:${novo.porta}/${novo.codigo}"

        status.text = tr("✅ Pronto! Deixe esta tela aberta")

        acoes.addView(criarTexto(tr("No computador, abra o navegador e digite:"), 16f, COR_TEXTO))

        acoes.addView(
            criarTexto(endereco, 24f, COR_AZUL, true).apply {
                setTextIsSelectable(true)
            }
        )

        adicionarBotao(
            acoes,
            criarBotao(tr("Copiar endereço"), null, COR_AZUL) {
                val area = getSystemService(ClipboardManager::class.java)
                area.setPrimaryClip(ClipData.newPlainText("Arquivos Pro", endereco))
                Toast.makeText(this, tr("Endereço copiado"), Toast.LENGTH_SHORT).show()
            }
        )

        acoes.addView(
            criarTexto(
                tr("• O computador precisa estar no mesmo Wi-Fi.\n• No navegador você pode baixar arquivos do celular e enviar arquivos do computador para qualquer pasta.\n• Só quem tiver o código do endereço consegue entrar.\n• Ao fechar esta tela, a transferência para."),
                14f
            )
        )

        rodape.removeAllViews()
        adicionarBotao(rodape, criarBotao(tr("Parar e sair"), null, COR_VERMELHO) { finish() })
        rodape.visibility = View.VISIBLE
    }

    private fun mostrarRecebidos() {

        val texto =
            tr("📥 Recebidos do computador:\n") +
                recebidos.take(8).joinToString("\n") { "• $it" }

        val existente = acoes.findViewWithTag<android.widget.TextView>("recebidos")

        if (existente != null) {
            existente.text = texto
        } else {
            acoes.addView(
                criarTexto(texto, 14f, COR_VERDE, true).apply { tag = "recebidos" }
            )
        }
    }
}
