#include <stdarg.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#if defined(_WIN32)
#define HIMARI_OS "windows"
#elif defined(__APPLE__)
#define HIMARI_OS "macos"
#elif defined(__linux__)
#define HIMARI_OS "linux"
#else
#error Unsupported operating system
#endif

#if defined(__x86_64__) || defined(_M_X64)
#define HIMARI_ARCH "x86_64"
#elif defined(__aarch64__) || defined(_M_ARM64)
#define HIMARI_ARCH "arm64"
#else
#error Unsupported architecture
#endif

#if defined(__clang__)
#define HIMARI_COMPILER "clang"
#define HIMARI_COMPILER_MAJOR __clang_major__
#define HIMARI_COMPILER_MINOR __clang_minor__
#define HIMARI_COMPILER_PATCH __clang_patchlevel__
#elif defined(__GNUC__)
#define HIMARI_COMPILER "gcc"
#define HIMARI_COMPILER_MAJOR __GNUC__
#define HIMARI_COMPILER_MINOR __GNUC_MINOR__
#define HIMARI_COMPILER_PATCH __GNUC_PATCHLEVEL__
#elif defined(_MSC_VER)
#define HIMARI_COMPILER "msvc"
#define HIMARI_COMPILER_MAJOR (_MSC_VER / 100)
#define HIMARI_COMPILER_MINOR (_MSC_VER % 100)
#define HIMARI_COMPILER_PATCH 0
#else
#define HIMARI_COMPILER "unknown"
#define HIMARI_COMPILER_MAJOR 0
#define HIMARI_COMPILER_MINOR 0
#define HIMARI_COMPILER_PATCH 0
#endif

typedef uint32_t fixture_flags;
typedef void *fixture_handle;

typedef struct fixture_pair {
    int32_t left;
    uint32_t right;
} fixture_pair;

typedef struct fixture_bits {
    unsigned int mode : 3;
    unsigned int ready : 1;
} fixture_bits;

typedef union fixture_value {
    int32_t integer;
    double floating;
} fixture_value;

typedef int32_t (*fixture_visit_callback)(fixture_pair value, void *context);

_Static_assert(sizeof(uint8_t) == 1, "uint8_t must occupy one byte");
_Static_assert(sizeof(int32_t) == 4, "int32_t must occupy four bytes");
_Static_assert(sizeof(uint32_t) == 4, "uint32_t must occupy four bytes");
_Static_assert(sizeof(unsigned int) == 4, "bitfield storage must occupy four bytes");

static fixture_pair make_pair(int32_t left, uint32_t right) {
    fixture_pair result = {left, right};
    return result;
}

static int32_t variadic_sum(int count, ...) {
    int32_t result = 0;
    va_list arguments;
    va_start(arguments, count);
    for (int index = 0; index < count; index++) {
        result += va_arg(arguments, int);
    }
    va_end(arguments);
    return result;
}

static int32_t fixture_callback(fixture_pair value, void *context) {
    const int32_t *base = (const int32_t *) context;
    return value.left + (int32_t) value.right + *base;
}

static int32_t invoke_callback(fixture_visit_callback callback, fixture_pair value, void *context) {
    return callback(value, context);
}

static unsigned int trailing_zero_count(uint32_t value) {
    unsigned int count = 0;
    while (value != 0 && (value & UINT32_C(1)) == 0) {
        value >>= 1;
        count++;
    }
    return count;
}

static unsigned int contiguous_bit_count(uint32_t value) {
    unsigned int count = 0;
    while ((value & UINT32_C(1)) != 0) {
        value >>= 1;
        count++;
    }
    return count;
}

int main(void) {
    const uint16_t byte_order_probe = UINT16_C(1);
    const char *byte_order = *((const uint8_t *) &byte_order_probe) == 1 ? "little_endian" : "big_endian";
    union {
        fixture_bits fields;
        uint32_t storage;
    } bits = {0};
    bits.fields.mode = 7;
    const uint32_t mode_mask = bits.storage;
    bits.storage = 0;
    bits.fields.ready = 1;
    const uint32_t ready_mask = bits.storage;
    const fixture_pair returned_pair = make_pair(-7, UINT32_C(42));
    const int32_t callback_base = 100;
    const fixture_pair callback_pair = {17, UINT32_C(25)};
    const int32_t callback_result = invoke_callback(fixture_callback, callback_pair, (void *) &callback_base);

    printf("{\n");
    printf("  \"protocolVersion\": 1,\n");
    printf("  \"fixtures\": [\"abi-minimum-layouts-v1\", \"abi-callback-conventions-v1\"],\n");
    printf("  \"target\": {\n");
    printf("    \"operatingSystem\": \"%s\",\n", HIMARI_OS);
    printf("    \"architecture\": \"%s\",\n", HIMARI_ARCH);
    printf("    \"byteOrder\": \"%s\",\n", byte_order);
    printf("    \"addressSize\": %llu,\n", (unsigned long long) sizeof(void *));
    printf("    \"addressAlignment\": %llu\n", (unsigned long long) _Alignof(void *));
    printf("  },\n");
    printf("  \"compiler\": {\"family\": \"%s\", \"major\": %d, \"minor\": %d, \"patch\": %d},\n",
            HIMARI_COMPILER,
            HIMARI_COMPILER_MAJOR,
            HIMARI_COMPILER_MINOR,
            HIMARI_COMPILER_PATCH);
    printf("  \"types\": [\n");
    printf("    {\"name\": \"u8\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(uint8_t), (unsigned long long) _Alignof(uint8_t));
    printf("    {\"name\": \"i32\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(int32_t), (unsigned long long) _Alignof(int32_t));
    printf("    {\"name\": \"u32\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(uint32_t), (unsigned long long) _Alignof(uint32_t));
    printf("    {\"name\": \"f64\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(double), (unsigned long long) _Alignof(double));
    printf("    {\"name\": \"const_u8_ptr\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(const uint8_t *), (unsigned long long) _Alignof(const uint8_t *));
    printf("    {\"name\": \"void_ptr\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(void *), (unsigned long long) _Alignof(void *));
    printf("    {\"name\": \"fixture_handle\", \"byteSize\": %llu, \"alignment\": %llu},\n",
            (unsigned long long) sizeof(fixture_handle), (unsigned long long) _Alignof(fixture_handle));
    printf("    {\"name\": \"fixture_flags\", \"byteSize\": %llu, \"alignment\": %llu}\n",
            (unsigned long long) sizeof(fixture_flags), (unsigned long long) _Alignof(fixture_flags));
    printf("  ],\n");
    printf("  \"aggregates\": [\n");
    printf("    {\"name\": \"fixture_pair\", \"byteSize\": %llu, \"alignment\": %llu, \"fields\": [",
            (unsigned long long) sizeof(fixture_pair), (unsigned long long) _Alignof(fixture_pair));
    printf("{\"name\": \"left\", \"byteOffset\": %llu, \"bitOffset\": null, \"bitWidth\": null}, ",
            (unsigned long long) offsetof(fixture_pair, left));
    printf("{\"name\": \"right\", \"byteOffset\": %llu, \"bitOffset\": null, \"bitWidth\": null}]},\n",
            (unsigned long long) offsetof(fixture_pair, right));
    printf("    {\"name\": \"fixture_bits\", \"byteSize\": %llu, \"alignment\": %llu, \"fields\": [",
            (unsigned long long) sizeof(fixture_bits), (unsigned long long) _Alignof(fixture_bits));
    printf("{\"name\": \"mode\", \"byteOffset\": 0, \"bitOffset\": %u, \"bitWidth\": %u}, ",
            trailing_zero_count(mode_mask), contiguous_bit_count(mode_mask >> trailing_zero_count(mode_mask)));
    printf("{\"name\": \"ready\", \"byteOffset\": 0, \"bitOffset\": %u, \"bitWidth\": %u}]},\n",
            trailing_zero_count(ready_mask), contiguous_bit_count(ready_mask >> trailing_zero_count(ready_mask)));
    printf("    {\"name\": \"fixture_value\", \"byteSize\": %llu, \"alignment\": %llu, \"fields\": [",
            (unsigned long long) sizeof(fixture_value), (unsigned long long) _Alignof(fixture_value));
    printf("{\"name\": \"integer\", \"byteOffset\": %llu, \"bitOffset\": null, \"bitWidth\": null}, ",
            (unsigned long long) offsetof(fixture_value, integer));
    printf("{\"name\": \"floating\", \"byteOffset\": %llu, \"bitOffset\": null, \"bitWidth\": null}]}\n",
            (unsigned long long) offsetof(fixture_value, floating));
    printf("  ],\n");
    printf("  \"callbacks\": [\n");
    printf("    {\"name\": \"fixture_visit_callback\", \"callingConvention\": \"system\", ");
    printf("\"pointerSize\": %llu, \"pointerAlignment\": %llu, \"invocationResult\": %d}\n",
            (unsigned long long) sizeof(fixture_visit_callback),
            (unsigned long long) _Alignof(fixture_visit_callback),
            callback_result);
    printf("  ],\n");
    printf("  \"checks\": {\"structureReturnLeft\": %d, \"structureReturnRight\": %u, \"variadicSum\": %d}\n",
            returned_pair.left, returned_pair.right, variadic_sum(3, 1, 2, 3));
    printf("}\n");
    return EXIT_SUCCESS;
}
