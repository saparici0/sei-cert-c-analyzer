#include <stdio.h>
#include <wchar.h>

void checkFileChar(FILE *file) {
    int c;
    while ((c = fgetc(file)) != EOF) {
        // Procesar el carácter leído
        printf("Character read: %c\n", c);
    }

    if (c == EOF) {
        printf("Reached end of file\n");
    }
}

void checkFileWChar(FILE *file) {
    wint_t wc;
    while ((wc = fgetwc(file)) != WEOF) {
        // Procesar el carácter leído
        wprintf(L"Character read: %lc\n", wc);
    }

    if (wc == WEOF) {
        wprintf(L"Reached end of file\n");
    }
}

int main() {
    FILE *file = fopen("test.txt", "r");
    if (file != NULL) {
        checkFileChar(file);
        checkFileWChar(file);
        fclose(file);
    } else {
        printf("Error opening file\n");
    }
    return 0;
}
