

package power;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.models.PowerModelCubic;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


public class PSO 
{
    Details dt=new Details();
    double weight=0.1;
    double c1=2;
    double c2=2;
    double x_max = 0;
    double v_max=0;
    double x_min =0.1;
    double v_min=0.1;
    int iter=10;
    
    PSO()
    {
        
    }
    
    public void applyPSO()
    {
        try
        {
            Random rn=new Random();
            
            
            dt.Velocity=new double[dt.pop][dt.vmlist.size()];
            dt.Position=new double[dt.pop][dt.vmlist.size()];
            
            x_max=dt.hostList.size();
            v_max=dt.hostList.size();
            
            
            for(int i=0;i<dt.pop;i++)
            {       
                String pp="";
                for(int j=0;j<dt.vmlist.size();j++)
                {
                    double e1=Math.round(x_min + (x_max - x_min) *rn.nextDouble());
                    double e2=Math.round(v_min + (v_max - v_min) *rn.nextDouble()) ;                
                    if(e1==0)
                        e1=1;
                    if(e1>dt.hostList.size())
                        e1=dt.hostList.size();
                    
                    if(e2==0)
                        e2=1;
                    if(e2>dt.hostList.size())
                        e2=dt.hostList.size();
                    
                    dt.Position[i][j]=e1;
                    dt.Velocity[i][j]=e2;                    
                    
                }                                     
            }
            
                        
            for(int it=0;it<iter;it++)                
            {
                for(int i=0;i<dt.pop;i++)
                {
                    String g=dt.population.get(i).toString();
                    String hh=fittnessFun(g);
                    String tg[]=hh.split("@");
                    double uti=Double.parseDouble(tg[0]);
                    System.out.println(g+" : "+uti);
                    g=tg[1];
                    if(dt.pbest>uti)
                    {
                        dt.pbestPop=g;
                        dt.pbest=uti;
                    }
                }
                
                if(dt.gbest>dt.pbest)
                {
                    dt.gbest=dt.pbest;
                    dt.gbestPop=dt.pbestPop;
                }
                System.out.println("best = "+dt.gbestPop+" : "+dt.gbest);
                System.out.println("======================= iter  "+it);
                
                dt.population=new ArrayList();
                
                for(int i=0;i<dt.pop;i++)
                {       
                    String pp="";
                    String rr1[]=dt.gbestPop.split("#");
                    
                    
                    for(int j=0;j<dt.vmlist.size();j++)
                    {
                        dt.Velocity[i][j]=weight*dt.Velocity[i][j]+c1*rn.nextDouble()*(Double.parseDouble(rr1[j])-dt.Position[i][j])+c2*rn.nextDouble()*(Double.parseDouble(rr1[j])-dt.Position[i][j]);                                                
                        dt.Position[i][j]=dt.Position[i][j]+dt.Velocity[i][j];
                        
                        double e1=Math.abs(dt.Position[i][j]);
                        
                        if(e1<1)
                            e1=1;
                        if(e1>dt.hostList.size())
                            e1=dt.hostList.size()-1;
                        
                        dt.Position[i][j]=e1;
                    
                    
                        PowerVm vm=dt.vmlist.get(j);                    
                        int s1=rn.nextInt(dt.hostList.size()-1);
                        PowerHost ht=dt.hostList.get(s1);
                        while(ht.isSuitableForVm(vm))
                        {
                            s1=rn.nextInt(dt.hostList.size());
                            ht=dt.hostList.get(s1);
                            break;
                        }
                        pp=pp+(s1+1)+"#";
                    }                     
                    pp=pp.substring(0, pp.lastIndexOf("#"));
                    dt.population.add(pp);
                   // System.out.println("new pop="+pp);
                }
                
            }
                
            
            System.out.println("final = "+dt.gbestPop+" : "+dt.gbest);
            dt.ppow=dt.gbest/(double)dt.hostList.size();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
        
    public String fittnessFun(String pop)
    {
        String res="";
        double uti=0;
        String newpop="";
        try
        {
            PowerVmAllocationPolicySimple ps=new PowerVmAllocationPolicySimple(dt.hostList);
            
            String g1[]=pop.split("#");
            for(int i=0;i<g1.length;i++)
            {
                int a1=Integer.parseInt(g1[i])-1; // host;
                PowerHost ph=dt.hostList.get(a1);
                PowerVm vm=dt.vmlist.get(i);
                boolean bool=ps.allocateHostForVm(vm, ph);
                
                if(bool)
                {        
                    newpop=newpop+g1[i]+"#";
                    uti=uti+ph.getUtilizationOfRam()+ph.getUtilizationOfBw()+ph.getUtilizationOfCpuMips();                    
                }
                else
                {
                    System.out.println("VM - "+vm.getId()+" is migrated");
                    for(int j=0;j<dt.hostList.size();j++)
                    {
                        if(j!=a1)
                        {
                            PowerHost ph1=dt.hostList.get(j);
                
                            boolean bool1=ps.allocateHostForVm(vm, ph1);
                            if(bool1)
                            {                        
                                newpop=newpop+(j+1)+"#";
                                uti=uti+ph1.getUtilizationOfRam()+ph1.getUtilizationOfBw()+ph1.getUtilizationOfCpuMips();                    
                                break;
                            }
                        }
                    }
                }
            }                                  
            res=uti+"@"+newpop;
            updateHost();
            
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return res;
    }
    
    public void updateHost()
    {
        try
        {
            dt.hostList=new ArrayList<PowerHost>(); // PM
            for(int i=0;i<dt.Ht.size();i++)
            {
                String a1[]=dt.Ht.get(i).toString().split("\t");
                
                int id=Integer.parseInt(a1[0]);
                int cpu=Integer.parseInt(a1[1]);
                int ram1=Integer.parseInt(a1[2]);
                int bw2=Integer.parseInt(a1[3]);
                int storage=100000;
                List<Pe> peList1 = new ArrayList<Pe>();
                int mips1 = cpu;//1000000;
                
                for(int k=0;k<cpu;k++)
                    peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); 
                //peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); 
                
                //dt.hostList.add(new Host(id, new RamProvisionerSimple(ram1),new BwProvisionerSimple(bw2), storage, peList1,new VmSchedulerTimeShared(peList1))); 
                dt.hostList.add(new PowerHost(id, new RamProvisionerSimple(ram1),new BwProvisionerSimple(bw2), storage, peList1,new VmSchedulerTimeShared(peList1),new PowerModelCubic(1000,500))); 
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
