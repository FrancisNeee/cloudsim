package nix.learning;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * CloudSimExample1，一个Datacener、Host、VM
 *
 * @Author FrancisNee
 * @Date 2024/04/20 15:56:22
 */
public class ExampleLearning1 {

    /**
     * Creates main() to run this example.
     *
     * @param args the args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Log.printLine("Starting CloudSimExample1...");

        try {
            // 用户数量，可以通过这个变量来控制并行请求的数量，以模拟不同负载条件下的云环境
            int num_user = 1;
            // 用于管理仿真的时间。在仿真开始之前，它被初始化为当前的日期和时间。这样可以确保仿真的时间从当前时刻开始，以便对事件进行准确的时间记录和调度。
            Calendar calendar = Calendar.getInstance();
            // 当设置为true时，CloudSim将记录和输出仿真过程中发生的所有事件。通常，在调试或详细分析模拟过程时，将其设置为true。在正常仿真过程中，通常将其保持为false，以减少输出并提高性能。
            boolean trace_flag = false;
            // 初始化
            CloudSim.init(num_user, calendar, trace_flag);
            // 创建数据中心
            Datacenter datacenter0 = createDatacenter("Datacenter_0");
            // 创建数据中心代理
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();
            // 创建VM
            List<Vm> vmlist = new ArrayList<>();
            int vmid = 0;
            int mips = 1000;
            long size = 10000;
            int ram = 512;
            long bw = 1000;
            int pesNumber = 1;
            String vmm = "Xen";
            Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmlist.add(vm);
            // 提交VM
            broker.submitVmList(vmlist);

            // 创建Cloudlet
            List<Cloudlet> cloudletList = new ArrayList<>();
            int id = 0;
            long length = 400000;
            long fileSize = 300;
            long outputSize = 300;
            // 创建了一个利用率模型对象，这个模型表示了任务在执行过程中的资源利用情况。在这个例子中，使用了UtilizationModelFull，表示任务在执行期间始终以最大速率利用资源。
            UtilizationModel utilizationModel = new UtilizationModelFull();
            Cloudlet cloudlet = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel,
                utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            cloudlet.setVmId(vmid);
            cloudletList.add(cloudlet);

            // 提交Cloudlet
            broker.submitCloudletList(cloudletList);

            // 开始仿真
            CloudSim.startSimulation();
            // 结束仿真
            CloudSim.stopSimulation();

            // 从Cloudlet打印仿真结果
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);

            Log.printLine("CloudSimExample1 finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    /**
     * 用于创建一个数据中心(Datacenter)，并配置其中的主机和资源。Datacenter用于模拟云环境中的物理资源。 Creates the datacenter.
     *
     * @param name the name
     * @return the datacenter
     */
    private static Datacenter createDatacenter(String name) {
        // Host：即物理机，PM
        List<Host> hostList = new ArrayList<>();

        // CPU内核，以mipse衡量
        List<Pe> peList = new ArrayList<>();
        int mips = 1000;
        // PeProvisioner用于将Host的Pe分配给VM
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        // 创建宿主机
        int hostId = 0;
        int ram = 2048;
        long storage = 1000000;
        int bw = 10000;
        hostList.add(new Host(hostId, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
            new VmSchedulerTimeShared(peList)));

        // DatacenterCharacteristics用于定义和存储一个数据中心的各种特性和配置信息。
        // 这个类的主要功能是提供一个详尽的描述数据中心的物理和经济属性，使仿真环境能够准确地模拟现实世界中数据中心的行为和性能。
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        // 使用处理资源的成本，单位为G$（GridSim货币单位）每PE（处理元素）时间单位
        double cost = 3.0;
        // 使用内存资源的成本，单位为G$每MB
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        // 使用带宽资源的成本，单位为G$每Mbps，此例中为0.0，意味着带宽使用是免费的。
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();
        // 数据中心可用的存储设备列表。此例中初始化为一个空的LinkedList，意味着没有添加SAN（存储区域网络）设备。
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
            cost, costPerMem, costPerStorage, costPerBw);

        Datacenter datacenter = null;
        try {
            // VmAllocationPolicySimple是一个基础的分配策略，通常将虚拟机简单地分配到有足够资源（CPU、内存等）的第一个可用主机上。hostList是包含数据中心所有主机的列表，这些主机是虚拟机可能被分配的目标。
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    /**
     * 创建一个数据中心代理（DatacenterBroker），DatacenterBroker负责管理虚拟机（VM）和云任务（cloudlets）的提交和调度。
     *
     * @return the datacenter broker
     */
    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the Cloudlet objects.
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine(
            "Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(
                    indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() + indent
                        + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(
                        cloudlet.getExecStartTime()) + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }
    }

}
