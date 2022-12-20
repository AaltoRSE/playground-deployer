package com.dockerKube.test.serviceImpl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dockerKube.beans.DeploymentBean;
import com.dockerKube.serviceImpl.KubeServiceImpl;
import com.dockerKube.utils.CommonUtil;
import com.dockerKube.utils.DockerKubeConstants;
import java.nio.file.Paths;


public class KubeServiceImplTest {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	@Test	
	public void getSolutionYMLFileTest() throws Exception{
		logger.info("getSolutionYMLFileTest Start");
		CommonUtil cutil=new CommonUtil();
		KubeServiceImpl impl=new KubeServiceImpl();
		String json=cutil.getFileDetails("blueprint.json");
		DeploymentBean dBean =new DeploymentBean();
		dBean.setProbeImage("repo/probeImage:1");
		dBean.setProbePort("5006");
		dBean.setBluePrintImage("repo/blueprintImage:1");
		dBean.setBluePrintPort("8555");
		dBean.setIncrementPort("8557");
		
		impl.getSolutionYMLFile(dBean, json);
		assertNotNull(dBean.getSolutionYml());
		logger.info("getSolutionYMLFileTest End");
		
	}
	@Test	
	public void createCompositeSolutionZipTest() throws Exception{
		logger.info("createCompositeSolutionZipTest Start");
		byte[] byteArray =null;
		DeploymentBean dBean =new DeploymentBean();
		KubeServiceImpl impl=new KubeServiceImpl();
        String path = Paths.get(".").toAbsolutePath().normalize().toString();
        dBean.setFolderPath(path);
		dBean.setBluePrintjson("blueprint.json");
		dBean.setDockerInfoJson("dockerinfo.json");
		dBean.setSolutionYml("solution.yml");
		dBean.setDataBrokerJson("dataBroker.json");
		dBean.setCmnDataPd("password");
		dBean.setCmnDataUrl("https://dataurl");
		dBean.setCmnDataUser("datauser");
		byteArray=impl.createCompositeSolutionZip(dBean);
		assertNotNull(byteArray);
		logger.info("createCompositeSolutionZipTest End");
		
	}
	@Test	
	public void getSolutionRevisionEnvTest() throws Exception{
		logger.info("getSolutionRevisionEnvTest Start");

		Map<String, String> solRevIdMap = new HashMap<String, String>();
		solRevIdMap.put("solution-id-x", "revision-id-x");
		solRevIdMap.put("solution-id-y", "revision-id-y");

		DeploymentBean dBean = new DeploymentBean();
		dBean.setSolutionId("solution-id-a");
		dBean.setSolutionRevisionId("revision-id-a");
		dBean.setSolutionRevisionIdMap(solRevIdMap);
		
		CommonUtil util = new CommonUtil();
		String dockerEnv = util.getEnvFileDetails(dBean);

		assertTrue("cannot find solutionId", 
			(dockerEnv.indexOf("SOLUTION_ID=solution-id-x,solution-id-y") != -1
			|| dockerEnv.indexOf("SOLUTION_ID=solution-id-y,solution-id-x") != -1));

		assertTrue("cannot find solution=revisionId entries", 
			(dockerEnv.indexOf("SOL_REVISION_ID=solution-id-x:revision-id-x,solution-id-y:revision-id-y") != -1
			|| dockerEnv.indexOf("SOL_REVISION_ID=solution-id-y:revision-id-y,solution-id-x:revision-id-x") != -1));

		logger.info("getSolutionRevisionEnvTest End");
		
	}
	@Test	
	public void getSingleSolutionYMLFileTest() throws Exception{
		logger.info("getSingleSolutionYMLFileTest Start");
		DeploymentBean dBean =new DeploymentBean();
		KubeServiceImpl impl=new KubeServiceImpl();
		dBean.setSingleModelPort("8556");
		dBean.setSingleTargetPort("3330");
		dBean.setDockerProxyHost("http://host");
		dBean.setDockerProxyPort("4243");
		impl.getSingleSolutionYMLFile("repo/image:1", "30333", dBean);
		logger.info("getSingleSolutionYMLFileTestt End");
	}
}
