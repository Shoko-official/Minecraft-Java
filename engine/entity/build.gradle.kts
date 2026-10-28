plugins {
    id("minecraft.java-conventions")
}

dependencies {
    api(project(":engine:world"))
    api(project(":engine:item"))
    api(project(":engine:loot"))
}
