import java.io.File
import java.lang.IllegalArgumentException

class XmlWriter(private val output: File) {
    var count = 0
    private var space:String = ""
        get() = "\t".repeat(count)
    private fun write(text:String){
        output.appendText(text)
    }
    fun writeTag(tag:String,name:String){
        write("$space<$tag> $name </$tag>\n")
    }
    private fun writeTag(tag:String,name:Char){
        val newName = name.toString()
            .replace("&", "&amp;")
            .replace("<","&lt;")
            .replace(">","&gt;")
            .replace("\"","&quot;")
        write("$space<$tag> $newName </$tag>\n")
    }
    fun writeTag(tag:String,name:Char,boolean: Boolean){
            writeTag(tag,name)
    }
    fun writeTag(tag:String,name:Char,check:Char=' ',any:Boolean=false){
        if (any || name == check){
            writeTag(tag,name)
        }
        else throw IllegalArgumentException()
    }
    fun writeStartTag(tag: String="tokens"){
        write("$space<$tag>\n")
        count ++
    }
    fun writeEndTag(tag: String="tokens"){
        count --
        write("$space</$tag>\n")
    }
}