package bspkrs.treecapitator.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import bspkrs.helpers.nbt.NBTTagCompoundHelper;
import bspkrs.helpers.nbt.NBTTagListHelper;
import bspkrs.treecapitator.config.TCSettings;
import bspkrs.treecapitator.fml.gui.GuiConfigCustomCategoryListEntry;
import bspkrs.treecapitator.util.Reference;
import bspkrs.util.ItemID;
import bspkrs.util.ListUtils;
import bspkrs.util.config.ConfigCategory;
import bspkrs.util.config.Configuration;
import bspkrs.util.config.Property;

public class ThirdPartyModConfig
{
    private final String                modID;
    private List<ItemID>                axeList;
    private List<ItemID>                shearsList;
    private boolean                     overrideIMC;
    private Map<String, TreeDefinition> treesMap;
    
    private boolean                     isChanged = false;
    
    /*
     * This special constructor provides the default vanilla tree "mod"
     */
    protected ThirdPartyModConfig(boolean init)
    {
        modID = Reference.MINECRAFT;
        overrideIMC = TCSettings.userConfigOverridesIMC;
        
        if (init)
        {
            axeList = new ArrayList<ItemID>(ToolRegistry.instance().vanillaAxeList());
            shearsList = new ArrayList<ItemID>(ToolRegistry.instance().vanillaShearsList());
            treesMap = TreeRegistry.instance().vanillaTrees();
        }
        else
        {
            axeList = new ArrayList<ItemID>();
            shearsList = new ArrayList<ItemID>();
            treesMap = new TreeMap<String, TreeDefinition>();
        }
    }
    
    protected ThirdPartyModConfig()
    {
        this(true);
    }
    
    protected ThirdPartyModConfig(String modID)
    {
        this.modID = modID;
        this.overrideIMC = TCSettings.userConfigOverridesIMC;
        this.axeList = new ArrayList<ItemID>();
        this.shearsList = new ArrayList<ItemID>();
        this.treesMap = new TreeMap<String, TreeDefinition>();
    }
    
    public void merge(ThirdPartyModConfig toMerge)
    {
        if (!this.modID.equals(toMerge.modID))
            throw new IllegalArgumentException(String.format("Cannot merge ThirdPartyModConfig objects with different modID values! this.modID: %s  toMerge.modID: %s", this.modID, toMerge.modID));
        
        this.overrideIMC = this.overrideIMC || toMerge.overrideIMC;
        
        for (ItemID itemID : toMerge.axeList)
            if (!axeList.contains(itemID))
                axeList.add(itemID);
        
        for (ItemID itemID : toMerge.shearsList)
            if (!shearsList.contains(itemID))
                shearsList.add(itemID);
        
        for (Entry<String, TreeDefinition> newEntry : toMerge.treesMap.entrySet())
        {
            if (this.treesMap.containsKey(newEntry.getKey()))
            {
                this.treesMap.get(newEntry.getKey()).appendWithSettings(newEntry.getValue());
                continue;
            }
            
            for (Entry<String, TreeDefinition> entry : this.treesMap.entrySet())
                if (newEntry.getValue().hasCommonLog(entry.getValue()))
                {
                    entry.getValue().appendWithSettings(newEntry.getValue());
                    continue;
                }
            
            this.treesMap.put(newEntry.getKey(), newEntry.getValue());
        }
        
        isChanged = true;
    }
    
    public static ThirdPartyModConfig readFromNBT(NBTTagCompound tpModCfg)
    {
        ThirdPartyModConfig tpmc = new ThirdPartyModConfig(tpModCfg.getString(Reference.MOD_ID));
        
        if (tpModCfg.hasKey(Reference.AXE_ID_LIST))
            for (ItemID itemID : ListUtils.getDelimitedStringAsItemIDList(tpModCfg.getString(Reference.AXE_ID_LIST), ";"))
                tpmc.addAxe(itemID);
        
        if (tpModCfg.hasKey(Reference.SHEARS_ID_LIST))
            for (ItemID itemID : ListUtils.getDelimitedStringAsItemIDList(tpModCfg.getString(Reference.SHEARS_ID_LIST), ";"))
                tpmc.addShears(itemID);
        
        NBTTagList treeList = NBTTagCompoundHelper.getTagList(tpModCfg, Reference.TREES, (byte) 10);
        
        for (int i = 0; i < treeList.tagCount(); i++)
        {
            NBTTagCompound tree = NBTTagListHelper.getCompoundTagAt(treeList, i);
            tpmc.addTreeDef(tree.getString(Reference.TREE_NAME), new TreeDefinition(tree));
        }
        
        tpmc.isChanged = true;
        
        return tpmc;
    }
    
    public void writeToNBT(NBTTagCompound tpModCfg)
    {
        tpModCfg.setString(Reference.MOD_ID, modID);
        if (axeList.size() > 0)
            tpModCfg.setString(Reference.AXE_ID_LIST, ListUtils.getListAsDelimitedString(axeList, "; "));
        if (shearsList.size() > 0)
            tpModCfg.setString(Reference.SHEARS_ID_LIST, ListUtils.getListAsDelimitedString(shearsList, "; "));
        
        NBTTagList treeList = new NBTTagList();
        for (Entry<String, TreeDefinition> e : treesMap.entrySet())
        {
            NBTTagCompound tree = new NBTTagCompound();
            e.getValue().writeToNBT(tree);
            tree.setString(Reference.TREE_NAME, e.getKey());
            treeList.appendTag(tree);
        }
        
        tpModCfg.setTag(Reference.TREES, treeList);
    }
    
    public static ThirdPartyModConfig readFromConfiguration(Configuration config, String category)
    {
        ConfigCategory cc = config.getCategory(category);
        cc.setCustomIGuiConfigListEntryClass(GuiConfigCustomCategoryListEntry.class);
        ThirdPartyModConfig tpmc = new ThirdPartyModConfig(config.get(category, Reference.MOD_ID, Reference.MINECRAFT, (String) null, Property.Type.MOD_ID)
                .setLanguageKey("bspkrs.tc.configgui." + Reference.MOD_ID).getString());
        if (cc.containsKey(Reference.AXE_ID_LIST))
            for (ItemID itemID : ListUtils.getDelimitedStringAsItemIDList(cc.get(Reference.AXE_ID_LIST).setLanguageKey("bspkrs.tc.configgui." + Reference.AXE_ID_LIST).getString(), ";"))
                tpmc.addAxe(itemID);
        if (cc.containsKey(Reference.SHEARS_ID_LIST))
            for (ItemID itemID : ListUtils.getDelimitedStringAsItemIDList(cc.get(Reference.SHEARS_ID_LIST).setLanguageKey("bspkrs.tc.configgui." + Reference.SHEARS_ID_LIST).getString(), ";"))
                tpmc.addShears(itemID);
        
        tpmc.overrideIMC = config.getBoolean(Reference.OVERRIDE_IMC, category, TCSettings.userConfigOverridesIMC, Reference.overrideIMCDesc,
                "bspkrs.tc.configgui." + Reference.OVERRIDE_IMC);
        
        for (ConfigCategory ctgy : cc.getChildren())
            tpmc.addTreeDef(ctgy.getName(), new TreeDefinition(config, ctgy.getQualifiedName()));
        
        return tpmc;
    }
    
    public void writeToConfiguration(Configuration config, String category)
    {
        config.get(category, Reference.MOD_ID, modID, (String) null, Property.Type.MOD_ID).setLanguageKey("bspkrs.tc.configgui." + Reference.MOD_ID);
        config.get(category, Reference.AXE_ID_LIST, ListUtils.getListAsDelimitedString(axeList, "; ")).setLanguageKey("bspkrs.tc.configgui." + Reference.AXE_ID_LIST);
        config.get(category, Reference.SHEARS_ID_LIST, ListUtils.getListAsDelimitedString(shearsList, "; ")).setLanguageKey("bspkrs.tc.configgui." + Reference.SHEARS_ID_LIST);
        config.getBoolean(Reference.OVERRIDE_IMC, category, overrideIMC, Reference.overrideIMCDesc, "bspkrs.tc.configgui." + Reference.OVERRIDE_IMC);
        
        for (Entry<String, TreeDefinition> e : treesMap.entrySet())
            if (!e.getKey().startsWith(category))
                e.getValue().writeToConfiguration(config, category + "." + e.getKey());
            else
                e.getValue().writeToConfiguration(config, e.getKey());
        
        config.setCategoryCustomIGuiConfigListEntryClass(category, GuiConfigCustomCategoryListEntry.class);
        
        this.isChanged = false;
    }
    
    public ThirdPartyModConfig addTreeDef(String key, TreeDefinition tree)
    {
        if (!treesMap.containsKey(key))
            treesMap.put(key, tree);
        else
            treesMap.get(key).appendWithSettings(tree);
        
        this.isChanged = true;
        return this;
    }
    
    public ThirdPartyModConfig addAxe(ItemID axe)
    {
        if (!this.axeList.contains(axe))
            this.axeList.add(axe);
        
        this.isChanged = true;
        return this;
    }
    
    public ThirdPartyModConfig addShears(ItemID shears)
    {
        if (!this.shearsList.contains(shears))
            this.shearsList.add(shears);
        
        this.isChanged = true;
        return this;
    }
    
    public ThirdPartyModConfig registerTrees()
    {
        for (Entry<String, TreeDefinition> e : treesMap.entrySet())
            TreeRegistry.instance().registerTree(e.getKey(), e.getValue());
        
        return this;
    }
    
    public ThirdPartyModConfig registerTools()
    {
        for (ItemID axe : axeList)
            if (!axe.id.trim().isEmpty())
                ToolRegistry.instance().registerAxe(axe);
        
        for (ItemID shears : shearsList)
            if (!shears.id.trim().isEmpty())
                ToolRegistry.instance().registerShears(shears);
        
        return this;
    }
    
    public String modID()
    {
        return modID;
    }
    
    public boolean overrideIMC()
    {
        return overrideIMC;
    }
    
    public ThirdPartyModConfig setOverrideIMC(boolean bol)
    {
        overrideIMC = bol;
        return this;
    }
    
    public boolean isChanged()
    {
        return isChanged;
    }
}
