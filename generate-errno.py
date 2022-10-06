from pathlib import Path

consts = []
objects = []
errno_branches = [None] * 133


def generate_from(filename: str):
    """
    Generates from the specified errno filename.
    """

    p = Path(filename)
    with open(p) as f:
        data = f.readlines()

    lines = [l for l in data if l.startswith("#define")]

    for line in lines:
        split = [s.rstrip("\n") for s in line.split("\t") if s]
        if len(split) < 2:
            continue

        name = split[1]
        txtnum = split[2]

        consts.append(f"public const val {name}: Int = {txtnum}")

        # use comment for name
        if len(split) < 4:
            continue

        ob_name = split[3].lstrip("/* ").rstrip(" */")
        ob_name = ''.join(x for x in ob_name.title() if x.isalnum())
        # skip aliased numbers
        try:
            number = int(txtnum)
            objects.append(f"public object {ob_name} : SyscallError({name})")

            # noinspection PyTypeChecker
            errno_branches[number] = ob_name
        except ValueError:
            continue


generate_from("/usr/include/asm-generic/errno-base.h")
generate_from("/usr/include/asm-generic/errno.h")

print("====")
print("Const block:")
print()
for i in consts:
    print(i)

print("\n\n\n\n\n\n")
print("Object block:")
print()
for i in objects:
    print(i)

print("\n\n\n\n\n\n")
print("List block:")
print()
print("public val ERRNO_MAPPING = mutableListOf(")
for idx, item in enumerate(errno_branches):
    if not item:
        print(f"    UnknownError({idx}),")
    else:
        print(f"    {item},")
print(")")
