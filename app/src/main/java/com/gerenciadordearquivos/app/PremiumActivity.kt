package com.gerenciadordearquivos.app

import android.os.Bundle
import android.view.View
import android.widget.Toast

/*
 * =============================================================
 * TELA DO PREMIUM (R$ 9,90, compra única)
 * =============================================================
 */

class PremiumActivity : TelaBase() {

    private val aoMudar: (Boolean) -> Unit = { ativo ->
        if (ativo) {
            Toast.makeText(this, "Obrigado! Premium ativado ⭐", Toast.LENGTH_LONG).show()
        }
        montar()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = "Faxina Premium"

        Premium.aoMudar(aoMudar)

        Premium.iniciar(this)

        montar()
    }

    override fun onDestroy() {

        Premium.removerOuvinte(aoMudar)

        super.onDestroy()
    }

    private fun montar() {

        val ativo = Premium.ativo(this)

        status.text =
            if (ativo) "⭐ Você já tem o Premium. Obrigado!"
            else "Pague uma vez e use para sempre"

        lista.visibility = View.GONE

        acoes.removeAllViews()

        acoes.addView(criarTexto("O que o Premium libera:", 18f, COR_TEXTO, true))

        listOf(
            "🔒  Cofre com PIN para esconder fotos e arquivos",
            "🖼️  Apagar fotos e vídeos duplicados com 1 toque",
            "🚫  Sem nenhum anúncio",
            "💛  Ajuda a manter o app sendo melhorado"
        ).forEach {
            acoes.addView(criarTexto(it, 16f, COR_TEXTO))
        }

        acoes.addView(
            criarTexto(
                "Compra única, sem assinatura. Vale para a sua conta Google: " +
                    "se trocar de celular, é só tocar em \"Restaurar compra\".",
                14f
            )
        )

        rodape.removeAllViews()

        if (!ativo) {

            adicionarBotao(
                rodape,
                criarBotao("Comprar Premium por ${Premium.preco()}", null, COR_DOURADO) {
                    Premium.comprar(this)?.let { erro ->
                        Toast.makeText(this, erro, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        adicionarBotao(
            rodape,
            criarBotao("Restaurar compra", null, COR_AZUL) {
                Premium.restaurar(this)
                Toast.makeText(this, "Verificando suas compras...", Toast.LENGTH_SHORT).show()
            }
        )

        rodape.visibility = View.VISIBLE
    }
}
