#include <stdio.h>

void read_uninitialized_memory() {
    int x;
    printf("Value of uninitialized x: %d\n", x);
}

int main() {
    read_uninitialized_memory();
    return 0;
}