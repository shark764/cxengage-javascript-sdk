const strings = require('serenova-js-utils/strings');
module.exports = {
  prompt: ({ inquirer, args }) =>
    inquirer
      .prompt([
        {
          type: 'input',
          name: 'name',
          message: `Enter the entityNameInCamelCase: \n`,
        },
        {
          type: 'list',
          name: 'apiType',
          choices: [
            {name: 'GET',value:'get'},
            {name: 'POST',value:'create'},
            {name: 'PUT',value:'update'},
            {name: 'DELETE',value:'delete'},
          ],
          message: `Choose an api method`,
        },
        {
          type: 'input',
          name: 'params',
          message: `Parameters: \n > Separate by comma: "active,name,description" \n > Leave blank and hit enter if no params required \n > No spaces \n`,
        },
    ]).then(answers => ({
      ...answers,
      functionName: strings.capitalizeFirstLetter(answers.name),
      kebabName: strings.camelCaseToKebabCase(answers.name),
      kebabNameNoLastLetter: strings.camelCaseToKebabCaseAndRemoveLastLetter(answers.name),
      normalName: strings.camelCaseToRegularForm(answers.name).toLowerCase(),
      pluralCheck: answers.name.charAt(answers.name.length) === 's',
    }))
    .then(answers =>
        inquirer.prompt(
          answers.params.split(',').map(param => ({
            type: 'confirm',
            name: param + '.required',
            message: `\n Is "${param}" a required parameter? \n`,
          })))
        .then(params1 =>
          inquirer.prompt(
            answers.params.split(',').map(param => ({
              type: 'list',
              name: param + '.type',
              choices: [
                {name: 'uuid',value:'uuid'},
                {name: 'boolean',value:'boolean'},
                {name: 'string',value:'string'},
                {name: 'object',value:'object'},
              ],
              message: `\n What is "${param}"'s type? \n`,
            })))
            .then(params2 => {
              for(key in params1) {
                params2[key].required = params1[key].required;
              }
              return params2
            }))
        .then(params => {
          const paramArray = Object.keys(params).map(key => ({name: key,type: params[key].type ,required: params[key].required}));
          const requiredParams = paramArray.filter(param => param.required);
          const optionalParams = paramArray.filter(param => !param.required);
          return {
            ...answers,
            docParams: paramArray.reduce((result,string,i) => result +  `;;   ${string.name}: {{${string.type}}} (${string.required? 'required' : 'optional'}),\n`, ''),
            reqSpecParams: requiredParams.reduce((result,string,i) => result +  `::specs/${strings.camelCaseToKebabCase(string.name)} `, ''),
            optSpecParams: optionalParams.reduce((result,string,i) => result +  `::specs/${strings.camelCaseToKebabCase(string.name)} `, ''),
          };
        }))}
