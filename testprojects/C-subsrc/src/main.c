#include <stdio.h>
#include <stdlib.h>

int main(void) {
#if FOO
	puts("FOO should not be defined.");
#endif
	puts("!!!Hello World!!!  ");
	return EXIT_SUCCESS;
}
