import org.jlab.groot.data.TDirectory
import org.jlab.groot.data.GraphErrors
import org.jlab.groot.data.H1F

// get list of sinphi hipo files
def inDir = "outhipo"
def inDirObj = new File(inDir)
def inList = []
def inFilter = ~/sinphi_.*\.hipo/
inDirObj.traverse( type: groovy.io.FileType.FILES, nameFilter: inFilter ) {
  if(it.size()>0) inList << inDir+"/"+it.getName()
}
inList.sort()
inList.each { println it }

/*
graphTree:
runnum
│
└ particle
  │
  ├ helicity+ : <sinphi> vs. filenum
  └ helicity- : <sinphi> vs. filenum
*/
def graphTree = [:]
def buildGraph = { tObj ->
  def grN = tObj.getName().tokenize('_').subList(0,3).join('_')
  def grT = tObj.getTitle().replaceAll(/ file.*$/," vs. file number")
  grT = grT.replaceAll(/sinPhi/,"<sinPhi>")
  def gr = new GraphErrors(grN)
  gr.setTitle(grT)
  return gr
}
  


def inTdir = new TDirectory()
def objList
def part,hel
def runnum,filenum
def tok
def obj
def graph
inList.each { inFile ->
  inTdir.readFile(inFile)
  objList = inTdir.getCompositeObjectList(inTdir)
  objList.each { objN ->
    if(objN.contains("/sinPhi_")) {
      obj = inTdir.getObject(objN)

      // tokenize histogram name to get runnum, filenum, particle type, and helicity
      tok = objN.tokenize('/')[-1].tokenize('_')
      part = tok[1]
      hel = tok[2]
      runnum = tok[3].toInteger()
      filenum = tok[4].toInteger()

      // initialize graph, if it hasn't been
      if(graphTree[runnum]==null) graphTree.put(runnum,[:])
      if(graphTree[runnum][part]==null) graphTree[runnum].put(part,[:])
      if(graphTree[runnum][part][hel]==null) {
        graphTree[runnum][part].put(hel,buildGraph(obj))
      }
      graph = graphTree[runnum][part][hel]

      // add <sinPhi> to the graph
      if(obj.integral()>0) {
        graph.addPoint(filenum,obj.getMean(),0,0)
      }
    }
  }

  inFile = null // "close" the file
}
    
// define output hipo file
def outHipo = new TDirectory()
graphTree.each { kRun,bRun ->
  outHipo.mkdir("/${kRun}")
  outHipo.cd("/${kRun}")
  bRun.each{ kPart,bPart ->
    bPart.each{ kHel,gr ->
      outHipo.addDataSet(gr)
    }
  }
}
def outHipoN = "outhipo/helicityPhi.hipo"
File outHipoFile = new File(outHipoN)
if(outHipoFile.exists()) outHipoFile.delete()
outHipo.writeFile(outHipoN)

