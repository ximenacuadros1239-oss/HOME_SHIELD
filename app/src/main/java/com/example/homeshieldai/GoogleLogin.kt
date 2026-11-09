package com.example.homeshieldai

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed class ResultadoGoogle {
    class Token(val idToken: String) : ResultadoGoogle()
    object Cancelado : ResultadoGoogle()
    class Error(val mensaje: String) : ResultadoGoogle()
}

suspend fun pedirTokenGoogle(context: Context): ResultadoGoogle {
    val opcion = GetSignInWithGoogleOption
        .Builder(context.getString(R.string.default_web_client_id))
        .build()
    val peticion = GetCredentialRequest.Builder()
        .addCredentialOption(opcion)
        .build()

    return try {
        val respuesta = CredentialManager.create(context).getCredential(context, peticion)
        val credencial = respuesta.credential
        if (credencial is CustomCredential &&
            credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            ResultadoGoogle.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
        } else {
            ResultadoGoogle.Error("No se pudo leer la cuenta de Google")
        }
    } catch (e: GetCredentialCancellationException) {
        ResultadoGoogle.Cancelado
    } catch (e: GetCredentialException) {
        ResultadoGoogle.Error("No se pudo iniciar con Google. Revisa que tengas una cuenta de Google en el celular.")
    } catch (e: GoogleIdTokenParsingException) {
        ResultadoGoogle.Error("Respuesta de Google inválida")
    }
}