package org.openengsb.config.editor.fields;

import java.util.Arrays;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.openengsb.config.jbi.types.ChoiceType;

public class DropdownChoiceField extends AbstractField {
    private static final long serialVersionUID = 1L;

    public DropdownChoiceField(String id, ChoiceType choiceType) {
        super(id, choiceType);
        DropDownChoice<String> ch = new DropDownChoice<String>("component", Arrays.asList(choiceType.getValues()));
        ch.setLabel(new Model<String>(choiceType.getName()));
        ch.setRequired(isRequired());
        add(ch);
    }
}