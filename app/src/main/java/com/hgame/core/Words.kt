package com.hgame.core

/** Word lists for the word games (common English words, written for this app). */
object Words {
    val FIVE: List<String> = """
        about above actor acute adopt adult after again agent agree ahead alarm album alert alike alive allow alone along alter
        among angel anger angle angry apart apple apply arena argue arise array aside asset audio avoid award aware badly baker
        basic beach began begin being below bench birth black blade blame blank blast blend blind block blood board boost booth
        bound brain brand brave bread break breed brief bring broad brown brush build built buyer cable candy carry catch cause
        chain chair chalk charm chart chase cheap check chest chief child china chose civil claim class clean clear click climb
        clock close cloud coach coast could count court cover craft crash cream crime cross crowd crown curve cycle daily dance
        dated dealt death debut delay depth doing doubt dozen draft drama drawn dream dress drink drive eager early earth eight
        elite empty enemy enjoy enter entry equal error event every exact exist extra faith false fancy fault favor fence field
        fifth fifty fight final first flame flash fleet floor fluid focus force forth forty forum found frame fresh front frost
        fruit fully funny ghost giant given glass globe glory grace grade grain grand grant grape grass great green greet gross
        group grown guard guess guest guide habit happy heart heavy hello hence horse hotel house human humor ideal image imply
        index inner input issue joint judge juice knife known label large laser later laugh layer learn lease least leave legal
        lemon level light limit local logic loose lover lower lucky lunch magic major maker march match maybe mayor meant medal
        media metal might minor mixed model money month moral motor mount mouse mouth movie music naked nerve never newly night
        noise north novel nurse ocean offer often olive onion order other ought outer owner paint panel paper party peace pearl
        phase phone photo piano piece pilot pitch place plain plane plant plate point pound power press price pride prime print
        prior prize proof proud prove queen quick quiet quite radio raise range rapid ratio reach ready realm relax reply right
        river robot rough round route royal rural salad scale scene scope score sense serve seven shade shake shape share sharp
        sheep sheet shelf shell shift shine shirt shock shoot short shown sight silly since sixth skill sleep slice slide small
        smart smile smoke snake solid solve sorry sound south space spare speak speed spend spent spice spoon sport staff stage
        stake stand start state steam steel stick still stock stone stood store storm story strip stuck study style sugar suite
        sunny super sweet table taken taste teach teeth thank theme there thick thing think third those three throw thumb tiger
        tight timer tired title toast today token topic total touch tough tower track trade train treat trend trial tribe trick
        truck truly trust truth twice uncle under union unity until upper upset urban usage usual valid value video virus visit
        vital voice waste watch water wheel where which while white whole whose woman world worry worth would write wrong yield
        young youth zebra
    """.trim().split(Regex("\\s+"))

    val HANGMAN: Map<String, List<String>> = mapOf(
        "Động vật" to "elephant giraffe penguin dolphin kangaroo butterfly squirrel crocodile octopus leopard rabbit turtle eagle panda hamster".split(" "),
        "Trái cây" to "banana pineapple mango strawberry watermelon cherry coconut grape orange peach papaya lychee durian avocado".split(" "),
        "Quốc gia" to "vietnam japan brazil canada france germany australia mexico egypt india thailand italy spain korea".split(" "),
        "Thể thao" to "football basketball tennis swimming volleyball badminton baseball cycling boxing hockey golf karate".split(" "),
        "Nghề nghiệp" to "teacher doctor engineer pilot farmer chef dentist lawyer artist singer police scientist".split(" "),
        "Đồ vật" to "umbrella keyboard backpack scissors lantern bicycle telescope pillow mirror candle notebook guitar".split(" "),
    )

    val SEARCH_THEMES: Map<String, List<String>> = mapOf(
        "Động vật" to "CAT DOG LION TIGER BEAR WOLF FROG DUCK GOAT HORSE MOUSE SHEEP ZEBRA CAMEL".split(" "),
        "Trái cây" to "APPLE MANGO PEAR LEMON GRAPE PEACH PLUM KIWI MELON BERRY LIME FIG CHERRY".split(" "),
        "Màu sắc" to "RED BLUE GREEN PINK GOLD GRAY BLACK WHITE BROWN CYAN PURPLE ORANGE YELLOW".split(" "),
        "Thời tiết" to "RAIN SNOW WIND STORM CLOUD SUNNY FOG HAIL MIST FROST THUNDER BREEZE".split(" "),
        "Không gian" to "STAR MOON SUN MARS EARTH ORBIT COMET ROCKET PLANET GALAXY VENUS SPACE".split(" "),
        "Biển" to "WAVE FISH SHARK CRAB WHALE CORAL SHELL SAND TIDE BOAT ISLAND OCEAN".split(" "),
    )

    val TYPING: List<String> = """
        the quick brown fox jumps over the lazy dog while the sun rises above the quiet hills
        practice makes perfect so keep typing every day and your speed will grow
        a journey of a thousand miles begins with a single step
        music fills the room as friends laugh and share stories late into the night
        bright stars shine over the ocean and waves roll softly on the sand
        coding games is fun when every small piece fits together nicely
        the little robot learned to paint colorful pictures of flowers and trees
        rain taps on the window while a cat sleeps beside a warm cup of tea
    """.trim().lines().map { it.trim() }
}
