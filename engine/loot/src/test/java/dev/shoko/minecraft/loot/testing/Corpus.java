package dev.shoko.minecraft.loot.testing;

import java.util.ArrayList;
import java.util.List;

/** Valid documents of every codec, the starting point of the mutation tests. */
public final class Corpus {

  private Corpus() {}

  public static List<String> tables() {
    return List.of(
        "{'type':'minecraft:block','random_sequence':'minecraft:blocks/stone','pools':[{'rolls':1,"
            + "'entries':[{'type':'minecraft:alternatives','children':["
            + "{'type':'minecraft:item','name':'minecraft:stone','condition':'minecraft:tool/can_silk_touch'},"
            + "{'type':'minecraft:item','name':'minecraft:cobblestone','condition':"
            + "{'type':'minecraft:survives_explosion'}}]}]}]}",
        "{'type':'minecraft:block','pools':[{'rolls':{'type':'minecraft:uniform','min':1,'max':2},"
            + "'condition':{'type':'minecraft:inverted','term':{'type':'minecraft:any_of','terms':"
            + "['minecraft:tool/can_shear','minecraft:tool/can_silk_touch']}},"
            + "'entries':[{'type':'minecraft:item','name':'minecraft:stick','condition':"
            + "{'type':'minecraft:table_bonus','enchantment':'minecraft:fortune',"
            + "'chances':[0.02,0.022222223,0.025,0.033333335,0.1]},'modifier':["
            + "{'type':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':2}},"
            + "{'type':'minecraft:explosion_decay'}]}]}]}",
        "{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item',"
            + "'name':'minecraft:beef','modifier':[{'type':'minecraft:set_count','count':"
            + "{'type':'minecraft:uniform','min':1,'max':3}},{'type':'minecraft:furnace_smelt',"
            + "'condition':{'type':'minecraft:entity_properties','entity':'this','predicate':"
            + "{'minecraft:flags':{'is_on_fire':true}}}},{'type':'minecraft:enchanted_count_increase',"
            + "'enchantment':'minecraft:looting','count':{'type':'minecraft:uniform','min':0.0,"
            + "'max':1.0}}]}]}]}",
        "{'type':'minecraft:fishing','pools':[{'rolls':1,'entries':["
            + "{'type':'minecraft:loot_table','value':'minecraft:gameplay/fishing/junk','weight':10,"
            + "'quality':-2},{'type':'minecraft:loot_table','value':{'type':'minecraft:chest',"
            + "'pools':[{'rolls':2,'entries':[{'type':'minecraft:empty','weight':3}]}]},"
            + "'weight':5,'quality':2,'condition':{'type':'minecraft:entity_properties','entity':'this',"
            + "'predicate':{'minecraft:type_specific/fishing_hook':{'in_open_water':true}}}}]}]}",
        "{'type':'minecraft:chest','modifier':{'type':'minecraft:discard'},'pools':[{'rolls':1,"
            + "'bonus_rolls':0.5,'entries':[{'type':'minecraft:group','children':["
            + "{'type':'minecraft:tag','items':'#minecraft:test_gems','expand':true},"
            + "{'type':'minecraft:dynamic','name':'minecraft:sherds'}]}]}]}");
  }

  public static List<String> conditions() {
    return List.of(
        "{'type':'minecraft:random_chance','chance':0.5}",
        "{'type':'minecraft:random_chance_with_enchanted_bonus','unenchanted_chance':0.0625,"
            + "'enchanted_chance':{'type':'minecraft:linear','base':0.13,'per_level_above_first':0.02},"
            + "'enchantment':'minecraft:looting'}",
        "{'type':'minecraft:all_of','terms':[{'type':'minecraft:killed_by_player'},"
            + "'minecraft:tool/can_shear',{'type':'minecraft:inverted','term':"
            + "{'type':'minecraft:survives_explosion'}}]}",
        "{'type':'minecraft:match_block','blocks':'minecraft:wheat','state':{'age':'7'}}",
        "{'type':'minecraft:match_tool','predicate':{'items':'#minecraft:test_gems','count':{'min':1},"
            + "'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch',"
            + "'levels':{'min':1}}]}}}",
        "{'type':'minecraft:location_check','offsetY':-1,'predicate':{'can_see_sky':true,"
            + "'block':{'blocks':'minecraft:stone'}}}",
        "{'type':'minecraft:entity_properties','entity':'attacker','predicate':{"
            + "'minecraft:entity_type':'#minecraft:undead','minecraft:flags':{'is_on_fire':true},"
            + "'minecraft:equipment':{'mainhand':{'items':'minecraft:shears'}},"
            + "'minecraft:movement':{'horizontal_speed':{'min':0.5}},'minecraft:periodic_tick':20}}",
        "{'type':'minecraft:damage_source_properties','predicate':{'tags':"
            + "[{'id':'minecraft:is_projectile','expected':true}],'is_direct':true}}",
        "{'type':'minecraft:weather_check','raining':true,'thundering':false}",
        "{'type':'minecraft:table_bonus','enchantment':'minecraft:fortune','chances':[0.1,0.25,1.0]}");
  }

  public static List<String> modifiers() {
    return List.of(
        "{'type':'minecraft:set_count','count':{'type':'minecraft:binomial','n':3,'p':0.5},'add':true}",
        "{'type':'minecraft:limit_count','limit':{'min':1,'max':4}}",
        "{'type':'minecraft:apply_bonus','enchantment':'minecraft:fortune',"
            + "'formula':'minecraft:binomial_with_bonus_count','parameters':{'extra':3,"
            + "'probability':0.5714286}}",
        "{'type':'minecraft:enchant_with_levels','levels':{'type':'minecraft:uniform','min':20,"
            + "'max':39},'options':'#minecraft:non_treasure'}",
        "{'type':'minecraft:enchant_randomly','options':['minecraft:sharpness','minecraft:smite']}",
        "{'type':'minecraft:set_enchantments','enchantments':{'minecraft:sharpness':3},'add':true}",
        "{'type':'minecraft:set_components','components':{'minecraft:custom_name':'Hello',"
            + "'!minecraft:lore':{}}}",
        "{'type':'minecraft:filtered','item_filter':{'predicates':{'minecraft:map_id':{}}},"
            + "'on_fail':{'type':'minecraft:discard'}}",
        "{'type':'minecraft:copy_components','source':'block_entity','include':['minecraft:a']}",
        "{'type':'minecraft:set_lore','lore':[],'mode':'append'}");
  }

  public static List<String> intProviders() {
    return List.of(
        "3",
        "{'type':'minecraft:uniform','min':0,'max':3}",
        "{'type':'minecraft:binomial','n':3,'p':0.5}",
        "'minecraft:cooking/time_coal'");
  }

  public static List<String> floatProviders() {
    return List.of(
        "0.5",
        "{'type':'minecraft:uniform','min':0.0,'max':1.0}",
        "{'type':'minecraft:enchantment_level','amount':{'type':'minecraft:linear','base':0.15,"
            + "'per_level_above_first':0.15}}",
        "'minecraft:cooking/speed_default'");
  }

  public static List<String> enchantments() {
    return new ArrayList<>(EnchantFixtures.definitions().values());
  }

  public static List<String> providers() {
    return List.of(
        "{'type':'minecraft:single','enchantment':'minecraft:silk_touch','level':1}",
        "{'type':'minecraft:by_cost_with_difficulty','enchantments':'#minecraft:non_treasure',"
            + "'min_cost':5,'max_cost_span':17}");
  }

  public static List<String> levelBased() {
    return List.of(
        "2",
        "{'type':'minecraft:linear','base':1.0,'per_level_above_first':0.5}",
        "{'type':'minecraft:lookup','values':[1.2,1.75],'fallback':"
            + "{'type':'minecraft:linear','base':1.5,'per_level_above_first':0.35}}",
        "{'type':'minecraft:clamped','value':{'type':'minecraft:levels_squared','added':1},"
            + "'min':1,'max':9}");
  }
}
