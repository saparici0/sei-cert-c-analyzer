#include <stdio.h>
#include <string.h>
#include <wchar.h>

void checkStringFunctions() {
    wchar_t wbuf[100];
    char buf[100];

    // Correcto: Usar funciones de cadenas estrechas con cadenas estrechas
    strcpy(buf, "test");
    strcat(buf, "test");
    printf("Length of narrow string: %lu\n", strlen(buf));
    if (strcmp(buf, "testtest") == 0) {
        printf("Narrow string equals 'testtest'\n");
    }

    // Correcto: Usar funciones de cadenas anchas con cadenas anchas
    wcscpy(wbuf, L"test");
    wcscat(wbuf, L"test");
    wprintf(L"Length of wide string: %lu\n", wcslen(wbuf));
    if (wcscmp(wbuf, L"testtest") == 0) {
        wprintf(L"Wide string equals 'testtest'\n");
    }
}

int main() {
checkStringFunctions();
    return 0;
}
