package com.gerenciadordearquivos.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/*
 * =============================================================
 * PREMIUM (compra única pela Google Play)
 *
 * Produto no Play Console:
 *   ID do produto: premium   (produto de compra única / "in-app")
 *   Preço: R$ 9,90
 *
 * O Premium libera: Cofre, remover fotos duplicadas, comprimir
 * fotos e app sem anúncios.
 * =============================================================
 */

object Premium {

    const val ID_PRODUTO = "premium"

    const val PRECO_PADRAO = "R$ 9,90"

    private const val PREFS = "premium"

    private const val CHAVE_ATIVO = "ativo"

    private var cliente: BillingClient? = null

    private var detalhes: ProductDetails? = null

    private val principal =
        Handler(Looper.getMainLooper())

    // Chamado quando o estado do Premium muda (compra/restauração)
    private val ouvintes =
        mutableListOf<(Boolean) -> Unit>()

    fun ativo(
        context: Context
    ): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(CHAVE_ATIVO, false)

    private fun salvar(
        context: Context,
        ativo: Boolean
    ) {

        val mudou =
            ativo(context) != ativo

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE_ATIVO, ativo)
            .apply()

        if (mudou) {
            principal.post {
                ouvintes.toList().forEach { it(ativo) }
            }
        }
    }

    fun aoMudar(
        ouvinte: (Boolean) -> Unit
    ) {
        ouvintes.add(ouvinte)
    }

    fun removerOuvinte(
        ouvinte: (Boolean) -> Unit
    ) {
        ouvintes.remove(ouvinte)
    }

    // Preço vindo da Play Store (ou o padrão se ainda não carregou)
    fun preco(): String =
        detalhes
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
            ?: PRECO_PADRAO

    // Conecta à Play Store e confere se o usuário já comprou
    fun iniciar(
        context: Context
    ) {

        val app =
            context.applicationContext

        if (cliente?.isReady == true) {
            verificarCompras(app)
            return
        }

        val novo =
            BillingClient.newBuilder(app)
                .setListener { resultado, compras ->

                    if (
                        resultado.responseCode ==
                        BillingClient.BillingResponseCode.OK &&
                        compras != null
                    ) {
                        processarCompras(app, compras)
                    }
                }
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()

        cliente = novo

        novo.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(
                    resultado: BillingResult
                ) {

                    if (
                        resultado.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {
                        verificarCompras(app)
                        carregarProduto()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Tenta de novo na próxima vez que a tela abrir
                }
            }
        )
    }

    private fun carregarProduto() {

        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(ID_PRODUTO)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

        cliente?.queryProductDetailsAsync(params) { _, resultado ->

            detalhes =
                resultado.productDetailsList.firstOrNull()
        }
    }

    private fun verificarCompras(
        context: Context
    ) {

        cliente?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { resultado, compras ->

            if (
                resultado.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {

                val comprou =
                    compras.any {
                        it.products.contains(ID_PRODUTO) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }

                salvar(context, comprou)

                processarCompras(context, compras)
            }
        }
    }

    private fun processarCompras(
        context: Context,
        compras: List<Purchase>
    ) {

        for (compra in compras) {

            if (
                !compra.products.contains(ID_PRODUTO) ||
                compra.purchaseState != Purchase.PurchaseState.PURCHASED
            ) {
                continue
            }

            salvar(context, true)

            // A Google devolve o dinheiro se a compra não for
            // confirmada em 3 dias
            if (!compra.isAcknowledged) {

                cliente?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(compra.purchaseToken)
                        .build()
                ) { }
            }
        }
    }

    // Abre a tela de pagamento da Google Play.
    // Devolve uma mensagem de erro, ou null se abriu.
    fun comprar(
        activity: Activity
    ): String? {

        val produto =
            detalhes

        val conectado =
            cliente?.isReady == true

        if (!conectado || produto == null) {

            iniciar(activity)

            return "A loja ainda está carregando. Verifique a internet " +
                "e tente de novo em alguns segundos."
        }

        val params =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(produto)
                            .build()
                    )
                )
                .build()

        val resultado =
            cliente!!.launchBillingFlow(activity, params)

        return if (
            resultado.responseCode ==
            BillingClient.BillingResponseCode.OK
        ) {
            null
        } else {
            "Não foi possível abrir o pagamento (${resultado.debugMessage})"
        }
    }

    fun restaurar(
        context: Context
    ) {
        iniciar(context)
    }
}
