#include <stdio.h>
#include <string.h>

void checkFgets(FILE *file) {
    char buf[100];
    if (fgets(buf, sizeof(buf), file)) {
        // Incorrecto: Asume que fgets devuelve una cadena no vacía
        printf("Read line: %s\n", buf);
    }
}

void checkFgetsCorrectly(FILE *file) {
    char buf[100];
    if (fgets(buf, sizeof(buf), file)) {
        // Correcto: Verifica si la cadena no está vacía antes de usarla
        if (strlen(buf) > 0) {
            printf("Read non-empty line: %s\n", buf);
        }
    }
}

void checkFgetws(FILE *file) {
    wchar_t buf[100];
    if (fgetws(buf, sizeof(buf)/sizeof(wchar_t), file)) {
        // Incorrecto: Asume que fgetws devuelve una cadena no vacía
        wprintf(L"Read line: %ls\n", buf);
    }
}

void checkFgetwsCorrectly(FILE *file) {
    wchar_t buf[100];
    if (fgetws(buf, sizeof(buf)/sizeof(wchar_t), file)) {
        // Correcto: Verifica si la cadena no está vacía antes de usarla
        if (wcslen(buf) > 0) {
            wprintf(L"Read non-empty line: %ls\n", buf);
        }
    }
}

int main() {
    FILE *file = fopen("test.txt", "r");
    if (file != NULL) {
        checkFgets(file);
        checkFgetsCorrectly(file);
        checkFgetws(file);
        checkFgetwsCorrectly(file);
        fclose(file);
    } else {
        printf("Error opening file\n");
    }
    return 0;
}
