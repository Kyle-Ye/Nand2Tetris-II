import java.util.*
import kotlin.collections.HashMap

class SymbolTable {
    private val symbolTable: HashMap<String, Int> = HashMap()

    init {
        for (i in 0..15) {
            addEntry("R$i", i)
        }
        addEntry("SCREEN", 16384)
        addEntry("KBD", 24576)
        addEntry("SP", 0)
        addEntry("LCL", 1)
        addEntry("ARG", 2)
        addEntry("THIS", 3)
        addEntry("THAT", 4)
    }

    fun addEntry(symbol: String, address: Int) {
        symbolTable[symbol] = address
    }

    operator fun contains(symbol: String): Boolean {
        return symbolTable.containsKey(symbol)
    }

    fun getAddress(symbol: String): Int {
        // Will be called after contains
        return symbolTable[symbol]!!
    }
}