fun dest(mnemonic: String): String {
    val destList = listOf("", "M", "D", "MD", "A", "AM", "AD", "AMD")
    return getIndexFromListInBinary(mnemonic, destList, 3)
}

fun comp(mnemonic: String): String {
    // 为最求效率，默认输入格式正确
    val a =
        if (mnemonic.contains("M")) {
            '1'
        } else {
            '0'
        }
    val compMap = mapOf(
        "0" to "101010",
        "1" to "111111",
        "-1" to "111010",
        "D" to "001100",
        "A" to "110000",
        "!D" to "001101",
        "!A" to "110001",
        "-D" to "001111",
        "-A" to "110011",
        "D+1" to "011111",
        "A+1" to "110111",
        "D-1" to "001110",
        "A-1" to "110010",
        "D+A" to "000010",
        "D-A" to "010011",
        "A-D" to "000111",
        "D&A" to "000000",
        "D|A" to "010101"
    )
    return a + compMap.getValue(mnemonic.replace("M", "A"))
}

fun jump(mnemonic: String): String {
    val jumpList = listOf("", "JGT", "JEQ", "JGE", "JLT", "JNE", "JLE", "JMP")
    return getIndexFromListInBinary(mnemonic, jumpList, 3)
}

class FormatErrorException(message: String) : Exception() {

}

private fun getIndexFromListInBinary(mnemonic: String, list: List<String>, n: Int = 3): String {
    val index = list.indexOf(mnemonic)
    if (index == -1) {
        throw FormatErrorException("$mnemonic can't be solved")
    }
    val binary = Integer.toBinaryString(index)
    return "0".repeat(n - binary.length) + binary
}