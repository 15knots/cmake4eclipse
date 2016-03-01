#include <stdio.h>
#include <stdlib.h>

int main(void) {
#if FOO-0
	puts("FOO should be != 0");
	int m= MAGIC;
#endif
	return EXIT_SUCCESS;
}
