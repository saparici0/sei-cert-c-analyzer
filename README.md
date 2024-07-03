# SEI CERT C Analyzer

## Descripción

**SEI CERT C Analyzer** es una herramienta diseñada para analizar el código en lenguaje C y verificar su conformidad con algunas de las recomendaciones de seguridad del CERT de SEI. Esta herramienta ayuda a identificar posibles vulnerabilidades mediante la aplicación de buenas prácticas.

## Integrantes

- Simon Aparicio
- Juliana De Castro
- William García
- Javier Toro

## Reglas tenidas en cuenta

- EXP33-C. Do not read uninitialized memory
- MSC32-C. Properly seed pseudorandom number generators
- FIO34-C. Distinguish between characters read from a file and EOF or WEOF
- FIO37-C. Do not assume that fgets() or fgetws() returns a nonempty string when successful
- STR38-C. Do not confuse narrow and wide character strings and functions
- POS47-C. Do not use threads that can be canceled asynchronously
- ENV32-C. All exit handlers must return normally
- ENV33-C. Do not call system()
- STR32-C. Do not pass a non-null-terminated character sequence to a library function that expects a string
- MEM34-C. Only free memory allocated dynamically
- MSC33-C. Do not pass invalid data to the asctime() function
- SIG30-C. Call only asynchronous-safe functions within signal handlers

## Referencias

- https://wiki.sei.cmu.edu/confluence/display/c/SEI+CERT+C+Coding+Standard

