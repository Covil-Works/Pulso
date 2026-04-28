# Pulso

Aplicativo Android para acompanhar pressão arterial no dia a dia, com foco em registro rápido, metas de acompanhamento e geração de relatório em PDF.

## O que o app faz

- Cadastro de perfil inicial (nome e idade).
- Registro de pressão arterial (sistólica/diastólica) com observações opcionais.
- Classificação automática dos registros por nível de risco (normal, alerta e risco).
- Dashboard com média dos últimos 7 dias e sequência de dias com registro.
- Histórico completo com opções de editar e excluir registros.
- Geração de relatório em PDF para compartilhar ou salvar.
- Configuração de metas e lembretes locais por dias da semana e horários.
- Calendário mensal com destaque dos dias com medições.

## Especificações Android

- `applicationId`: `com.covildev.pulso`
- `minSdk`: `24` (Android 7.0 Nougat)
- `targetSdk`: `36`
- `compileSdk`: `36`
- `versionCode`: `1`
- `versionName`: `1.0`
- Java: `17`
- Kotlin JVM target: `17`

## Stack do projeto

- Kotlin
- Jetpack Compose (Material 3)
- AndroidX
- Hilt (injeção de dependência)
- Room (persistência local)
- KSP

## Como rodar

1. Abra o projeto no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Execute o app em um emulador/dispositivo Android com API 24+.
