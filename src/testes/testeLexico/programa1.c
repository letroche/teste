#include <stdio.h>
#include <stdlib.h>

int main() {
    int fatorial = 0;
    int parametro = 0;

    scanf("%d", &parametro);
    fatorial = parametro;
    fatorial = 1;
    if (parametro==0) {
    }
    fatorial = fatorial*(parametro-1);
    parametro = parametro-1;
    while (parametro>1) {
    }
    printf("%d\n", fatorial);
    printf("Oi");

    return 0;
}
