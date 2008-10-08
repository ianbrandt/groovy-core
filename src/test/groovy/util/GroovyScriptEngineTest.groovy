/*
	@author Chuck Tassoni
*/
package groovy.util;

import java.awt.Dimension;

class GroovyScriptEngineTest extends GroovyTestCase{

	private File currentDir
	private File srcDir;
	private File script
	private File com
	private File company
	private File util
	private File makeMeSuper
	private File makeMe
	private File helperIntf
	private File helper

    /**
    * Here we have inheritance and delegation-- where the delegate implements an
    * interface-- all used by a dynamically instantiated class named 'MakeMe'.  
    */
	public void setUp(){
		locateCurrentDir();
		srcDir = new File(currentDir, 'dynamicSrcRootToBeDeleted')
		srcDir.mkdir();
		
		script = new File(srcDir, 'script.groovy')
		script << """
		    def obj = dynaInstantiate.instantiate(className, getClass().getClassLoader())
		    obj.modifyWidth(dim, addThis)
		    returnedMessage = obj.message
		"""
		
		com = new File(srcDir, 'com')
		com.mkdir()
		company = new File(com, 'company')
		company.mkdir()
		
		makeMeSuper = new File(company, "MakeMeSuper.groovy")
		makeMeSuper << """
		    package com.company
		    import com.company.util.*
		    class MakeMeSuper{
		       private HelperIntf helper = new Helper()
		       def getMessage(){
		       		helper.getMessage()
		       }
		    }    
		 """
		
		makeMe = new File(company, "MakeMe.groovy")
		makeMe << """
		    package com.company
		    import java.awt.Dimension
		    class MakeMe extends MakeMeSuper{
		       def modifyWidth(dim, addThis){
		          dim.width += addThis
		       }
		    }    
		 """
		 
		 util = new File(company, 'util')
		 util.mkdir()
		 
		 helperIntf = new File(util, "HelperIntf.groovy")
		 helperIntf << """
		    package com.company.util
		    interface HelperIntf{
		       public String getMessage();
		    }    
		 """
		 
		 helper = new File(util, "Helper.groovy")
		 helper << """
		    package com.company.util
		    class Helper implements HelperIntf{
		       public String getMessage(){
		       	  'worked'
		       }
		    }    
		 """
	}
	
	public void tearDown(){
	    try{
	    	helperIntf.delete()
	    	helper.delete()
	    	util.delete()
	    	makeMeSuper.delete()
			makeMe.delete()
 			company.delete()
 			com.delete()
 			script.delete()
 			srcDir.delete()
 		}catch(Exception ex){
 			throw new RuntimeException("Could not delete entire dynamic tree inside " + currentDir, ex)
 		}
	}

	public void testDynamicInstantiation() throws Exception{
		//Code run in the script will modify this dimension object.
    	Dimension dim = new Dimension();
    	
    	String[] roots = new String[1]
    	roots[0] = srcDir.getAbsolutePath()
    	GroovyScriptEngine gse = new GroovyScriptEngine(roots);
    	Binding binding = new Binding();
    	binding.setVariable("dim", dim);
    	binding.setVariable("dynaInstantiate", this);
    	
    	binding.setVariable("className", "com.company.MakeMe");
    	
    	int addThis = 3;
    	binding.setVariable("addThis", addThis);
    	
    	gse.run("script.groovy", binding);
    	
    	//The script instantiated com.company.MakeMe via our own
    	//instantiate method.  The instantiated object modified the
    	//width of our Dimension object, adding the value of our
    	//'addThis' variable to it.
    	assertEquals(new Dimension(addThis,0), dim);
    	
    	assertEquals('worked', binding.getVariable("returnedMessage") )
	}
	
	/*
	 * The script passes the className of the class it's supposed to
	 * instantiate to this method, expecting a newly instantiated object
	 * in return.  The reason this is not done in the script is that
	 * we want to ensure that no unforeseen problems occur if 
	 * the instantiation is not actually done inside the script,
	 * since real-world usages will likely require delegating that
	 * job.
	 */
	public Object instantiate(String className, ClassLoader classLoader){
		Class clazz = null;
		try {
			clazz = Class.forName(className, true, classLoader) ;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class.forName failed for  " + className, ex);
		}
		try {
			return clazz.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Could not instantiate object of class " + className, ex);
		}		
		
	}
	
	private void locateCurrentDir(){
		String bogusFile = "bogusFile";
	   	File f = new File(bogusFile);
	   	String path = f.getAbsolutePath();
	   	path = path.substring(0, path.length() - bogusFile.length());
	   	currentDir = new File(path);
	}

}