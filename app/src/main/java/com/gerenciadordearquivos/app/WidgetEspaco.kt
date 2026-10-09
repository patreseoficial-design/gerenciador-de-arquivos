package com.gerenciadordearquivos.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/*
 * =============================================================
 * WIDGET: ESPAÇO LIVRE
 * Mostra quanto espaço sobra no celular. Tocar abre a Análise.
 * =============================================================
 */

class WidgetEspaco : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        gerenciador: AppWidgetManager,
        ids: IntArray
    ) {
        atualizar(context, gerenciador, ids)
    }

    companion object {

        fun atualizarTodos(context: Context) {

            val gerenciador = AppWidgetManager.getInstance(context)

            val ids =
                gerenciador.getAppWidgetIds(
                    ComponentName(context, WidgetEspaco::class.java)
                )

            if (ids.isNotEmpty()) atualizar(context, gerenciador, ids)
        }

        private fun atualizar(
            context: Context,
            gerenciador: AppWidgetManager,
            ids: IntArray
        ) {

            val espaco = Armazenamento.espaco(Armazenamento.raizCelular)

            val abrir =
                PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, MainActivity::class.java)
                        .putExtra(AvisoArmazenamento.EXTRA_ABRIR_ANALISE, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            for (id in ids) {

                val tela = RemoteViews(context.packageName, R.layout.widget_espaco)

                if (espaco != null && espaco.total > 0) {

                    val usado = (espaco.usado * 100 / espaco.total).toInt()

                    tela.setTextViewText(
                        R.id.widgetLivre,
                        "${AvisoArmazenamento.formatar(espaco.livre)} livres"
                    )

                    tela.setTextViewText(
                        R.id.widgetDetalhe,
                        "$usado% usado de ${AvisoArmazenamento.formatar(espaco.total)}"
                    )

                    tela.setProgressBar(R.id.widgetBarra, 100, usado, false)
                }

                tela.setOnClickPendingIntent(R.id.widgetRaiz, abrir)

                gerenciador.updateAppWidget(id, tela)
            }
        }
    }
}
