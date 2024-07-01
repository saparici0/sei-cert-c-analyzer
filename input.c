#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

void *threadFunc(void *arg) {
    while (1) {
        // Hilo ejecutando
    }
    return NULL;
}

int main() {
    pthread_t thread;
    int res = pthread_create(&thread, NULL, threadFunc, NULL);
    if (res != 0) {
        perror("Thread creation failed");
        exit(EXIT_FAILURE);
    }

    // Incorrecto: Cancelar hilo de manera asíncrona
    pthread_cancel(thread);

    res = pthread_join(thread, NULL);
    if (res != 0) {
        perror("Thread join failed");
        exit(EXIT_FAILURE);
    }

    return 0;
}
