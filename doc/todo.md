# TODO

- [x] Mandar un solo mensaje cuando se solicita un tipo de archivo.
- [x] El solicitante debe parser el mensaje para ver si tiene más de un nombre de archivos.
- [x] Preguntar sobre si se debe descargar de varios peers a la vez.
- [x] Descargar diferentes tipos de archivos. (pdf: iText)
- [x] En el switch hacer un case 3 donde maneje la recepción del archivo.
- [x] Refactorizar los behaviours.
- [x] Agregar el documento descargado al catálogo del solicitante.
- [x] Se debe modificar el número de resultados dependiendo del número de resultados que devuelve un solo distribuidor? (Debe contar la cantidad de archivos que devuelve.)
- [x] Que el programa elija el mejor peer para hacer la transferencia de una y que el solicitante lo agrega a su propio catalogo.
   - [x] Se puede elegir automaticamente el mejor peer.
   - [x] El mejor peer puede ser el que conteste mas rapido.
   - [x] El mejor peer puede ser el que tenga el archivo mas descargado.
- [ ] Leer sobre parallel behaviour.
- [ ] Opciones: minishell, leer de un archivo los archivos a buscar.
- [x] Hacer otro behaviour para las transferencias.
- [x] Eliminir.


# Pruebas
- [x] Misma PC
    - [x] Probar un solo distribuidor y un solo solicitante
    - [x] Probar un solo distribuidor y uno o varios solicitante
    - [x] Probar un uno o varios distribuidor y un solicitante
    - [x] Probar encadenamiento de solicitudes
    - [x] Combinaciones de solicitudes simples
    - [x] Combinaciones de solicitudes simples con compuestas
    - [x] Combinaciones de solicitudes rechazadas

- [ ] Dos PC distintas
    - [ ] Probar un solo distribuidor y un solo solicitante
    - [ ] Probar un solo distribuidor y uno o varios solicitante
    - [ ] Probar un uno o varios distribuidor y un solicitante
    - [ ] Probar encadenamiento de solicitudes
    - [ ] Combinaciones de solicitudes simples
    - [ ] Combinaciones de solicitudes simples con compuestas
    - [ ] Combinaciones de solicitudes rechazadas
