#include <stdio.h>
#include <string.h>
#include <wchar.h>

void checkStringFunctions() {
    wchar_t wbuf[100];
    char buf[100];

    // Incorrecto: Usar funciones de cadenas estrechas con cadenas anchas
    strcpy((char *)wbuf, "test");
    strcat((char *)wbuf, "test");
    printf("Length of wide string: %lu\n", strlen((char *)wbuf));
    if (strcmp((char *)wbuf, "test") == 0) {
        printf("Wide string equals 'test'\n");
    }

    // Incorrecto: Usar funciones de cadenas anchas con cadenas estrechas
    wcscpy((wchar_t *)buf, L"test");
    wcscat((wchar_t *)buf, L"test");
    wprintf(L"Length of narrow string: %lu\n", wcslen((wchar_t *)buf));
    if (wcscmp((wchar_t *)buf, L"test") == 0) {
        wprintf(L"Narrow string equals 'test'\n");
    }
}

int main() {
    checkStringFunctions();
    return 0;
}
