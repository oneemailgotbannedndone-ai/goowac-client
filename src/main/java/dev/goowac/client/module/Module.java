package dev.goowac.client.module;
public final class Module {
    private final String name;
    private final ModuleCategory category;
    private final String description;
    private final boolean functional;
    private boolean enabled;
    public Module(String name, ModuleCategory category, String description, boolean functional) {
        this.name = name; this.category = category; this.description = description; this.functional = functional;
    }
    public String name() { return name; }
    public ModuleCategory category() { return category; }
    public String description() { return description; }
    public boolean functional() { return functional; }
    public boolean enabled() { return enabled; }
    public void toggle() { if (functional) enabled = !enabled; }
    public void setEnabled(boolean enabled) { this.enabled = functional && enabled; }
}
