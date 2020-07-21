/***
 *      _____   U _____ u   _   _        _  __        _                    
 *     |_ " _|  \| ___"|/  | \ |"|      |"|/ /    U  /"\  u       ___      
 *       | |     |  _|"   <|  \| |>     | ' /      \/ _ \/       |_"_|     
 *      /| |\    | |___   U| |\  |u   U/| . \\u    / ___ \        | |      
 *     u |_|U    |_____|   |_| \_|      |_|\_\    /_/   \_\     U/| |\u    
 *     _// \\_   <<   >>   ||   \\,-. ,-,>> \\,-.  \\    >>  .-,_|___|_,-. 
 *    (__) (__) (__) (__)  (_")  (_/   \.)   (_/  (__)  (__)  \_)-' '-(_/  
 */

Discord: Inia#7065


TODO: 
- Datapack isnt 100% complete
- Limited zone's packets updated but new zones arnt implemented (i let the id in the classes as commentary)
- pow2 some enchant route changed

I do not give our launcher / api it mean you will have to change the whole auth system in order to login, but it could be a test for your l2jdev, within 1 hour it should be done. (or you can reverse engin. my auth system to create your own launcher implementing the same protocol)

Aswell tenkai didnt have as plan to be retail so yep you will find non retail things.

 
I  will maybe put public my homunculus sources soon aswell if this repo got some merge requests


Team:

- Inia: Dev & Datapack
- Meruril: Datapack

I updated from mobius free pow1 sources which mean some of their bugs are still there but i fixed some as 
- the dual class inifinite certifs
- the artifact window not implemented is now implemented and working
- agathions slots are now properly working (primary & secondary abilites) before u did have both whatever slots they were
- Some provoke skills which should only provoke mobs were also provoking players have been fixed
- much more

The pow3 packets should be 100% done but i may be have forgot something since i updated the whole thing in one night, you can imagine i didnt test everything but
we didnt encounter any problems during our closed beta (2 weeks).

As i said i used mobius pow1 free to start but as you can see i had fun touching the whole project and as result it became quite different than mobius which mean you probably wont be able to get their next updates.
Second point is i made these sources to be runnable directly from intellij idea (10x faster for dev purposes), you can run your prod server from idea its not a problem but you have to know its not optimized / made for that.
And finally i added kotlin to the srcs for some classes
I wrote those three points to warn you: these srcs arn't easy to handle, you have to know what you're doing and have a competent team with you.
