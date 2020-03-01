import kotlin.collections.HashMap

private data class Property(val type: String,val kind: Kind,val index:Int)

class SymbolTable{
    private val table = HashMap<String,Property>()
    private val countTable = HashMap<Kind,Int>()
    fun startSubroutine(){
        for(key in table.filterValues { it.kind in setOf(Kind.ARGUMENT,Kind.VAR) }.keys) {
            table.remove(key)
        }
        countTable.remove(Kind.VAR)
        countTable.remove(Kind.ARGUMENT)
    }
    fun define(name: String,type:String,kind:Kind){
        val property = Property(type,kind,varCount(kind))
        table[name] = property
    }
    private fun varCount(kind: Kind):Int{
        countTable[kind] = countTable.getOrDefault(kind,-1)+1
        return countTable[kind]!!
    }
    operator fun contains(name: String):Boolean = name in table
    fun kindOf(name:String):Kind = table[name]!!.kind;
    fun typeOf(name: String):String = table[name]!!.type
    fun indexOf(name: String):Int = table[name]!!.index
}