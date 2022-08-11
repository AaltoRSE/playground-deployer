/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */
package com.dockerKube.kubernetesclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Properties;

@SpringBootApplication
@ComponentScan(basePackages = {"com.dockerKube.*"})
public class KubernetesClientApplication {
	public static final String CONFIG_ENV_VAR_NAME = "SPRING_APPLICATION_JSON";
	public static Logger logger = LoggerFactory.getLogger("init");
	public static void main(String[] args)throws Exception {
		final String springApplicationJson = System.getenv(CONFIG_ENV_VAR_NAME);
		if (springApplicationJson != null && springApplicationJson.contains("{")) {
			final ObjectMapper mapper = new ObjectMapper();
			// ensure it's valid
			mapper.readTree(springApplicationJson);
			logger.info("main: successfully parsed configuration from environment {}", CONFIG_ENV_VAR_NAME);
		} else {
			logger.info("main: no configuration found in environment {}", CONFIG_ENV_VAR_NAME);
		}
		try {
			Properties manifest = new Properties();
			manifest.load(KubernetesClientApplication.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
			manifest.entrySet().stream().forEach(e -> logger.info(e.toString()));
		} catch(Exception x) {
			logger.warn("no version information");
			x.printStackTrace();
		}

		SpringApplication.run(KubernetesClientApplication.class, args);
	}


}
