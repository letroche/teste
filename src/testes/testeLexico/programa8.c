#include <stdio.h>
#include <stdlib.h>

int main() {
    float fatorial = 0;
    int parametro = 0;

    scanf("%d", &parametro);
    fatorial = parametro;
    fatorial = 1;
    if (parametro!=0 || parametro==0) {
    fatorial = 1;
    }
    fatorial = fatorial*(parametro-1);
    parametro = parametro-12.456;
    while (parametro>1) {
    }
    printf("%f\n", fatorial);
    printf("Oi");

    return 0;
}
