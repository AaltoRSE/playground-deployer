import os
import yaml
import subprocess
import argparse
import logging
import shlex
import time
import subprocess

from kubernetes import client, config

logging.basicConfig(level=logging.INFO)


class NodeManager:
    def __init__(self, namespace):
        self.namespace = namespace
        self.logger = logging.getLogger("NodeManager")
        self.logger.info("NodeManager class initialized")
        
    def get_pods_names(self):
        pods_names = []
        for pod in self.__get_pods():
            pods_names.append(self._get_pod_name(pod))
        return pods_names

    def wait_until_ready(self, timeout_seconds=120):
        for time_passed in range(timeout_seconds):
            process = subprocess.run(
                ["kubectl", "-n", self.namespace, "get", "pods", "--field-selector=status.phase!=Running"],
                check=True, stdout=subprocess.PIPE, universal_newlines=True)
            out = process.stdout
            if len(out) == 0:
                return
            print("out =", out)
            self.logger.info(f"Pipeline {self.namespace} is not ready yet. Waiting {str(time_passed)}/{str(timeout_seconds)} seconds ..")
            time.sleep(1)

    def _get_pod_name(self, pod):
        return pod.metadata.name
    def _is_terminating(self, pod):
        pod_name = self._get_pod_name(pod)
        return self.is_terminating(pod_name=pod_name)

    def is_terminating(self, pod_name):
        process = subprocess.run(
            ["kubectl", "-n", self.namespace, "get", "pod", pod_name],
            check=True, stdout=subprocess.PIPE, universal_newlines=True)
        out = process.stdout
    def __get_pods(self):
        config.load_kube_config()
        v1 = client.CoreV1Api()
        # ToDo namespaced_service??
        return v1.list_namespaced_pod(namespace=self.namespace).items

class Jupyter:
    def __init__(self, namespace, base_path):
        self.__namespace=namespace
        self.base_path=base_path
        self.logger = logging.getLogger("Jupyter")
        self.logger.info("Juypter class initialized")

    def prepare_jupyter(self):
        self._send_protos_to_jupyter()
        self._send_deployment_to_jupyter()



    def _send_deployment_to_jupyter(self):
        # self.__wait_until_ready()
        self.logger.info("Pipeline._send_deployment_to_jupyter() ..")
        
        yamls_path = self.__get_yamls_path()+"/"
        self.logger.info(f"yamls_path = {yamls_path}")
        self._send_to_jupyter(source=yamls_path, destination="jupyter_connect_tools/deployments")

    def _send_protos_to_jupyter(self):
        # self.__wait_until_ready()
        self.logger.info("Pipeline._send_protos_to_jupyter() ..")
        
        protofiles_path = self.__get_protofiles_path()+"/"
        self.logger.info(f"protofiles_path = {protofiles_path}")
        self._send_to_jupyter(source=protofiles_path, destination="jupyter_connect_tools/microservice")

    def _send_to_jupyter(self, source, destination):
        # self.__wait_until_ready()
        self.logger.info("Pipeline._send_files_to_jupyter() ..")

        pod_name_jupyter = self._get_pod_name_jupyter()
        self.logger.info(f"pod_name_jupyter: {pod_name_jupyter}")
        if pod_name_jupyter is None:
            raise Exception("pod_name_jupyter is None!")

        if pod_name_jupyter is None:
            return
        try:
            shared_folder = self.__get_shared_folder_path()
            self.logger.info("shared_folder = " + shared_folder)
            
            destination_full = pod_name_jupyter + ":" + shared_folder + f"/{destination}/"
        except:
            destination_full = pod_name_jupyter + f":/home/jovyan/{destination}/"
        self.logger.info(f"destination = {destination_full}")
        cmd = f"kubectl -n {self.__get_namespace()} cp {source} {destination_full}"
        self._runcmd(cmd)

    def __get_shared_folder_path(self):
        self.logger.info("get_shared_folder_path")
        yaml_files = self.__get_yamls()
        self.logger.info(f"yaml_files = {yaml_files}")
        for yaml_file in yaml_files:
            with open(yaml_file, "r") as f:
                data = yaml.load(f, Loader=yaml.FullLoader)
            try:
                environment_variables = data["spec"]["template"]["spec"]["containers"][0]["env"]
                for environment_variable in environment_variables:
                    if environment_variable["name"] == "SHARED_FOLDER_PATH":
                        shared_folder_path = environment_variable["value"]
                        return shared_folder_path
            except:
                pass

    def _get_pod_name_jupyter(self):
        #ToDo Define final image name.
        self.logger.info("_get_pod_name_jupyter()")
        JUPYTER_IMAGES = ["cicd.ai4eu-dev.eu/graphene/jupyter-connect:1.1","cicd.ai4eu-dev.eu/graphene/jupyter-connect:latest"]

        image_names, container_names_yaml = self.__get_image_container_names()

        container_name = None
        for image_name, container_name_yaml in zip(image_names, container_names_yaml):
            if image_name in JUPYTER_IMAGES:
                container_name = container_name_yaml
                self.logger.info(f"Jupyter Image = {image_name}")
                break
        if container_name is None:
            self.logger.error("error in Jupyter._get_pod_name_jupyter()!!")
            return None
        pod_names = self._get_node_manager().get_pods_names()
        for pod_name in pod_names:
            self.logger.info(f"name = {pod_name}")
            if container_name in pod_name:
                if(self._get_node_manager().is_terminating(pod_name)):
                    continue
                self.logger.info(f"pod_name = {pod_name} \n\n\n")
                return pod_name
        self.logger.error("error in Jupyter._get_pod_name_jupyter()!!")
        return None

    def _get_node_manager(self):
        return NodeManager(self.__get_namespace())


    def __get_image_container_names(self):
        image_names = []
        container_names = []
        yaml_files = self.__get_yamls()
        for yaml_file in yaml_files:
            with open(yaml_file, "r") as f:
                data = yaml.load(f, Loader=yaml.FullLoader)
            try:
                containers = data["spec"]["template"]["spec"]["containers"]
                for container in containers:
                    container_name = container["name"]
                    image_name = container["image"]
                    container_names.append(container_name)
                    image_names.append(image_name)
            except:
                pass
        return image_names, container_names

    def __get_yamls(self):
        path_yamls = self.__get_yamls_path()
        files = os.listdir(path_yamls)
        files_yaml = [file for file in files if self._is_yaml_file(file)]
        yamls = [os.path.join(path_yamls,file) for file in files_yaml]
        return yamls

    def _is_yaml_file(self, file):
        return file.endswith(".yaml") or file.endswith("yml")

    def __wait_until_ready(self, timeout_seconds=120):
        self._get_node_manager().wait_until_ready(timeout_seconds)

    def __get_yamls_path(self):
        return os.path.join(self.base_path, "deployments")

    def __get_protofiles_path(self):
        return os.path.join(self.base_path, "microservice")

    def __get_namespace(self):
        return self.__namespace

    def _runcmd(self, cmd):
        args = shlex.split(cmd)
        process = subprocess.run(args, check=True, capture_output=True, text=True)
        return process.stdout


def main():
    my_parser = argparse.ArgumentParser()
    my_parser.add_argument('--namespace', '-n', action='store', type=str, required=True,
                           help='name of namespace is required ')
    my_parser.add_argument('--base_path'         , '-bp',  action='store', type=str, required=False, default=os.getcwd(),
                           help='basepath of solution')
    args = my_parser.parse_args()

    namespace = args.namespace
    base_path=args.base_path
    print(f"base_path = {base_path}")

    j = Jupyter(namespace=namespace, base_path=base_path)
    j.prepare_jupyter()

if __name__ == '__main__':
    main()