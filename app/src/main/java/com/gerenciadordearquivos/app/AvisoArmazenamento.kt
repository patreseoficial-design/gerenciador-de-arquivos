package com.gerenciadordearquivos.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/*
 * =============================================================
 * AVISO DE CELULAR QUASE CHEIO
 * Uma vez por dia confere o espaço livre. Se estiver abaixo de
 * 10% (ou de 2 GB), mostra uma notificação com atalho para a
 * Análise. No máximo um aviso a cada 3 dias. Também atualiza
 * o widget de espaço livre.
 * =============================================================
 */

object AvisoArmazenamento {

    const val CANAL = "espaco"

    private const val TRABALHO = "verificar_espaco"

    private const val PREFS = "preferencias"

    private const val CHAVE_ATIVO = "aviso_espaco"

    private const val CHAVE_ULTIMO = "ultimo_aviso"

    // A MainActivity abre a Análise quando recebe este extra
    const val EXTRA_ABRIR_ANALISE = "abrir_analise"

    fun ativo(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(CHAVE_ATIVO, true)

    fun definir(context: Context, ativo: Boolean) {

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE_ATIVO, ativo)
            .apply()

        agendar(context)
    }

    fun agendar(context: Context) {

        val trabalho =
            PeriodicWorkRequestBuilder<VerificarEspacoWorker>(1, TimeUnit.DAYS)
                .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                TRABALHO,
                ExistingPeriodicWorkPolicy.KEEP,
                trabalho
            )
    }

    fun criarCanal(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal =
                NotificationChannel(
                    CANAL,
                    tr("Celular quase cheio"),
                    NotificationManager.IMPORTANCE_DEFAULT
                )

            canal.description = tr("Avisa quando o armazenamento estiver acabando")

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    fun verificar(context: Context) {

        WidgetEspaco.atualizarTodos(context)

        if (!ativo(context)) return

        val espaco = Armazenamento.espaco(Armazenamento.raizCelular) ?: return

        val pouco =
            espaco.total > 0 &&
                (espaco.livre * 100 / espaco.total < 10 ||
                    espaco.livre < 2L * 1024 * 1024 * 1024)

        if (!pouco) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val agora = System.currentTimeMillis()

        if (agora - prefs.getLong(CHAVE_ULTIMO, 0) < TimeUnit.DAYS.toMillis(3)) return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        criarCanal(context)

        val abrir =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_ABRIR_ANALISE, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val livre = formatar(espaco.livre)

        val notificacao =
            NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(R.drawable.ic_acao_lixeira)
                .setContentTitle(tr("Seu celular está quase cheio"))
                .setContentText(tr("Só {0} livres. Toque para liberar espaço com o Arquivos Pro.", livre))
                .setContentIntent(abrir)
                .setAutoCancel(true)
                .build()

        try {
            NotificationManagerCompat.from(context).notify(1, notificacao)
            prefs.edit().putLong(CHAVE_ULTIMO, agora).apply()
        } catch (_: SecurityException) {
        }
    }

    fun formatar(bytes: Long): String {

        val gb = bytes / (1024.0 * 1024 * 1024)

        return if (gb >= 1) String.format(java.util.Locale.getDefault(), "%.1f GB", gb)
        else String.format(java.util.Locale.getDefault(), "%.0f MB", bytes / (1024.0 * 1024))
    }
}

class VerificarEspacoWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        AvisoArmazenamento.verificar(applicationContext)

        return Result.success()
    }
}
