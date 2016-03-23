package template_configurator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import com.petpet.c3po.api.model.helper.Filter;
import play.Logger;

@Root
public class TemplateController {
    public TemplateController(File file) {
        loadFromFile(file);

    }

    public TemplateController() {

    }

    public List<TemplateFilter> getFilters() {
        return filters;
    }

    public void loadFromFile(File file) {
        Serializer serlzr = new Persister();
        TemplateController configRead = null;
        try {
            configRead = serlzr.read(TemplateController.class, file);
            if (configRead != null) {
                setFilters(configRead.getFilters());
                setTemplates(configRead.getTemplates());
            }
        } catch (Exception e) {
            Logger.debug("No template config file was found. Using default values");
        }


    }

    public void setFilters(List<TemplateFilter> filters) {
        this.filters = filters;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    @ElementList
    List<TemplateFilter> filters;
    @ElementList
    List<Template> templates;

    @Override
    public int hashCode() {
        int result = 0;
        for (TemplateFilter filt : filters)
            result += filt.hashCode();
        for (Template templt : templates)
            result += templt.hashCode();
        return result;
    }

    public String[] getProps(Map<String, String> filter) {
        TemplateFilter tmp_filter = find_corresponding_template_filter(filter);
        Template tmp_template = find_template(tmp_filter);
        List<String> result = get_list_of_properties(tmp_template);
        return result.toArray(new String[0]);
    }

    public String getCurrentTemplate(Map<String, String> filter) {
        TemplateFilter tmp_filter = find_corresponding_template_filter(filter);
        Template template = find_template(tmp_filter);
        if (template != null)
            return template.getName();
        return "Default";

    }

    private List<String> get_list_of_properties(Template tmp_template) {
        List<String> result = new ArrayList<String>();
        if (tmp_template == null)
            return result;
        for (TemplateProperty prop : tmp_template.properties)
            result.add(prop.name);
        return result;
    }

    private Template find_template(TemplateFilter tmp_filter) {
        if (tmp_filter == null || templates == null)
            return null;
        for (Template tmpl : templates) {
            if (tmpl.ID.equals(tmp_filter.template_ID))
                return tmpl;
        }
        return null;
    }

    private TemplateFilter find_corresponding_template_filter(Map<String, String> filter) {
        if (filters != null) {
            for (TemplateFilter filt : filters) {
                if (filt.equals(filter))
                    return filt;
            }
        }
        return null;
    }

    public ArrayList<String> toArrayString() {
        ArrayList<String> result = new ArrayList<String>();
        if (templates != null) {
            for (Template t : templates) {
                result.add(t.toString());
            }
        }
        return result;
    }

    public boolean isSet() {
        return (templates != null && !templates.isEmpty());
    }

    public String[] getDefaultTemplate() {
        if (templates != null) {
            for (Template tmpl : templates) {
                if (tmpl.getName().equals("Default_template")) {
                    List<String> result = get_list_of_properties(tmpl);
                    return result.toArray(new String[0]);
                }
            }
        }
        String[] defaultProps = {"content_type", "valid", "format"};
        return defaultProps;

    }

}
