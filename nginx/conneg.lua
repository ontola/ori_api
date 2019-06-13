-- From https://github.com/EmptyStar/conneg/blob/master/conneg.lua

-- Define global conneg object
conneg = {}

-- Define charsets for parsing
conneg._charsets = {
  alpha = (function()
    local charset = {}
    for c in string.gmatch("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",".") do
      charset[c] = true
    end
    return charset
  end)(),

  digits = (function()
    local charset = {}
    for c in string.gmatch("1234567890",".") do
      charset[c] = true
    end
    return charset
  end)(),

  alphanum = (function()
    local charset = {}
    for c in string.gmatch("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890",".") do
      charset[c] = true
    end
    return charset
  end)(),

  token = (function()
    local charset = {}
    for c in string.gmatch("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-",".") do
      charset[c] = true
    end
    return charset
  end)(),

  typeChars = (function()
    local charset = {}
    for c in string.gmatch("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-+._!#$&^",".") do
      charset[c] = true
    end
    return charset
  end)()
}

-- Content negotiation for the HTTP Accept header
conneg.accept = {
  -- Functions that represent individual parsing states
  _state = {
    START = function(v,c)
      v:start()
      if conneg._charsets.typeChars[c] then
        v:captureTypeChar(c)
        return "TYPE"
      elseif c == "*" then
        v:captureTypeChar(c)
        return "ANY_TYPE"
      elseif c == " " then
        return "WHITESPACE_AFTER_START"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    TYPE = function(v,c)
      if conneg._charsets.typeChars[c] then
        v:captureTypeChar(c)
        return "TYPE"
      elseif c == "/" then
        return "TYPE_SLASH"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    ANY_TYPE = function(v,c)
      if c == "/" then
        return "ANY_SLASH"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    TYPE_SLASH = function(v,c)
      if conneg._charsets.typeChars[c] then
        v:captureSubtypeChar(c)
        return "SUBTYPE"
      elseif c == "*" then
        v:captureSubtypeChar(c)
        return "ANY_SUBTYPE"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    ANY_SLASH = function(v,c)
      if c == "*" then
        v:captureSubtypeChar(c)
        return "ANY_SUBTYPE"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    ANY_SUBTYPE = function(v,c)
      if c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_TYPE"
      else
        return "FAILSCAN"
      end
    end,

    SUBTYPE = function(v,c)
      if conneg._charsets.typeChars[c] then
        v:captureSubtypeChar(c)
        return "SUBTYPE"
      elseif c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_TYPE"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_SEMICOLON = function(v,c)
      if c == " " then
        return "WHITESPACE_AFTER_SEMICOLON"
      elseif c == "q" then
        return "PARAMETER_Q1"
      elseif conneg._charsets.token[c] then
        v:captureParameterName(c)
        return "PARAMETER_NAME"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_Q1 = function(v,c)
      if c == "=" then
        return "QVALUE_EQUALS"
      elseif conneg._charsets.token[c] then
        v:captureParameterName(c)
        return "PARAMETER_NAME"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_NAME = function(v,c)
      if conneg._charsets.token[c] then
        v:captureParameterName(c)
        return "PARAMETER_NAME"
      elseif c == "=" then
        return "PARAMETER_EQUALS"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_EQUALS = function(v,c)
      if conneg._charsets.token[c] then
        v:captureParameterValue(c)
        return "PARAMETER_VALUE"
      elseif c == "\"" then
        return "PARAMETER_BEGIN_QUOTE"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_VALUE = function(v,c)
      if conneg._charsets.token[c] then
        v:captureParameterValue(c)
        return "PARAMETER_VALUE"
      elseif c == "," then
        v:captureParameter()
        v:captureType()
        return "START"
      elseif c == ";" then
        v:captureParameter()
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        v:captureParameter()
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_BEGIN_QUOTE = function(v,c)
      if conneg._charsets.token[c] then
        v:captureParameterValue(c)
        return "PARAMETER_QUOTED_VALUE"
      elseif c == "," then
        return "START"
      else
       return "FAILSCAN"
      end
    end,

    PARAMETER_QUOTED_VALUE = function(v,c)
      if conneg._charsets.token[c] then
        v:captureParameterValue(c)
        return "PARAMETER_QUOTED_VALUE"
      elseif c == "\"" then
        v:captureParameter()
        return "PARAMETER_END_QUOTE"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    PARAMETER_END_QUOTE = function(v,c)
      if c == "," then
        v:captureType()
        return "START"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      else
        return "FAILSCAN"
      end
    end,

    QVALUE_EQUALS = function(v,c)
      if c == "0" then
        v:captureQ(c)
        return "Q0"
      elseif c == "1" then
        v:captureQ(c)
        return "Q1"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    Q0 = function(v,c)
      if c == "." then
        v:captureQ(c)
        return "Q0_DOT"
      elseif c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        v:captureParameter()
        return "PARAMETER_SEMICOLON"
      elseif q == " " then
        v:captureParameter()
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    Q1 = function(v,c)
      if c == "." then
        v:captureQ(c)
        return "Q1_DOT"
      elseif c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    Q1_DOT = function(v,c)
      if c == "0" then
        v:captureQ(c)
        return "Q1_DIGIT1"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    Q1_DIGIT1 = function(v,c)
      if c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      elseif c == "0" then
        v:captureQ(c)
        return "Q1_DIGIT2"
      else
        return "FAILSCAN"
      end
    end,

    Q1_DIGIT2 = function(v,c)
      if c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      elseif c == "0" then
        v:captureQ(c)
        return "Q1_DIGIT3"
      else
        return "FAILSCAN"
      end
    end,

    Q1_DIGIT3 = function(v,c)
      if c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    Q0_DOT = function(v,c)
      if conneg._charsets.digits[c] then
        v:captureQ(c)
        return "Q0_DIGIT1"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    Q0_DIGIT1 = function(v,c)
      if conneg._charsets.digits[c] then
        v:captureQ(c)
        return "Q0_DIGIT2"
      elseif c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    Q0_DIGIT2 = function(v,c)
      if conneg._charsets.digits[c] then
        v:captureQ(c)
        return "Q0_DIGIT3"
      elseif c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    Q0_DIGIT3 = function(v,c)
      if c == "," then
        v:captureType()
        return "START"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      else
        return "FAILSCAN"
      end
    end,

    WHITESPACE_AFTER_START = function(v,c)
      if c == " " then
        return "WHITESPACE_AFTER_START"
      elseif conneg._charsets.typeChars[c] then
        v:captureTypeChar(c)
        return "TYPE"
      elseif c == "*" then
        v:captureTypeChar(c)
        return "ANY_TYPE"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    WHITESPACE_AFTER_TYPE = function(v,c)
      if c == " " then
        return "WHITESPACE_AFTER_TYPE"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == "," then
        v:captureType()
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    WHITESPACE_AFTER_SEMICOLON = function(v,c)
      if c == " " then
        return "WHITESPACE_AFTER_SEMICOLON"
      elseif c == "q" then
        return "PARAMETER_Q1"
      elseif conneg._charsets.token[c] then
        v:captureParameterName(c)
        return "PARAMETER_NAME"
      elseif c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    WHITESPACE_AFTER_PARAMETER = function(v,c)
      if c == " " then
        return "WHITESPACE_AFTER_PARAMETER"
      elseif c == ";" then
        return "PARAMETER_SEMICOLON"
      elseif c == "," then
        v:captureParameter()
        v:captureType()
        return "START"
      else
        return "FAILSCAN"
      end
    end,

    FAILSCAN = function(v,c)
      if c == "," then
        return "START"
      else
        return "FAILSCAN"
      end
    end
  },

  -- Functions called to terminate parsing
  _terminate = {
    START = function(v) end,
    TYPE = function(v) end,
    ANY_TYPE = function(v) end,
    TYPE_SLASH = function(v) end,
    ANY_SLASH = function(v) end,
    ANY_SUBTYPE = function(v)
      v:captureType()
    end,
    SUBTYPE = function(v)
      v:captureType()
    end,
    ANY_SUBTYPE = function(v)
      v:captureType()
    end,
    SUBTYPE = function(v)
      v:captureType()
    end,
    PARAMETER_SEMICOLON = function(v) end,
    PARAMETER_NAME = function(v) end,
    PARAMETER_EQUALS = function(v) end,
    PARAMETER_VALUE = function(v)
      v:captureParameter()
      v:captureType()
    end,
    PARAMETER_BEGIN_QUOTE = function(v) end,
    PARAMETER_QUOTED_VALUE = function(v) end,
    PARAMETER_END_QUOTE = function(v)
      v:captureType()
    end,
    QVALUE_EQUALS = function(v) end,
    Q0 = function(v)
      v:captureType()
    end,
    Q1 = function(v)
      v:captureType()
    end,
    Q1_DOT = function(v) end,
    Q1_DIGIT1 = function(v)
      v:captureType()
    end,
    Q1_DIGIT2 = function(v)
      v:captureType()
    end,
    Q1_DIGIT3 = function(v)
      v:captureType()
    end,
    Q0_DOT = function(v) end,
    Q0_DIGIT1 = function(v)
      v:captureType()
    end,
    Q0_DIGIT2 = function(v)
      v:captureType()
    end,
    Q0_DIGIT3 = function(v)
      v:captureType()
    end,
    WHITESPACE_AFTER_START = function(v) end,
    WHITESPACE_AFTER_TYPE = function(v)
      v:captureType()
    end,
    WHITESPACE_AFTER_SEMICOLON = function(v) end,
    WHITESPACE_AFTER_PARAMETER = function(v) end,
    FAILSCAN = function(v) end
  },

  -- Function for advancing the parser state
  _step = function(next,v,c)
    return conneg.accept._state[next](v,c)
  end,

  -- Function and map for capturing registered sets of MIME types
  _registeredTypes = {},
  register = function(name,types)
    conneg.accept._registeredTypes[name] = conneg.accept._parse(types)
  end,

  -- Function that returns the most preferable value between the values
  -- requested and the values available
  negotiate = function(want,have)
    want = conneg.accept._parse(want)
    have = conneg.accept._registeredTypes[have]

    for x,request in pairs(want) do
      for y,type in pairs(have) do
        if request.type == "*" then return type:str() end
        if request.type == type.type then
          if request.subtype == "*" then return type:str() end
          if request.subtype == type.subtype then
            if request.parameterCount > 0 then
              local haveAllParameters = true
              for wantName,wantValue in pairs(request.parameters) do
                if wantValue ~= type.parameters[wantName] then
                  haveAllParameters = false
                  break
                end
              end
              if haveAllParameters then return type:str() end
            else
              return type:str()
            end
          end
        end
      end
    end
    return nil
  end,

  -- Function for sorting parsed values by descending preference
  _sort = function(t)
    table.sort(t,function(a,b)
      if a.type == "*" and b.type ~= "*" then return false end
      if a.type ~= "*" and b.type == "*" then return true end
      if a.type == b.type then
        if a.subtype == "*" and b.subtype ~= "*" then return false end
        if a.subtype ~= "*" and  b.subtype == "*" then return true end
        if a.subtype == b.subtype then
          if a.parameterCount > b.parameterCount then return true end
          if b.parameterCount > a.parameterCount then return false end
        end
      end
      if a.q > b.q then return true end
      if b.q > a.q then return false end
      return a.index < b.index
    end)
  end,

  -- Function for parsing a header value
  _parse = function(header)
    -- Create object for holding successfully parsed types
    local types = {}

    -- Create object for capturing parsed values
    local values = {
     currentState = "START",
     index = 0,

      start = function(v)
        v.current = {
          index = v.index,
          type = "",
          subtype = "",
          parameterName = "",
          parameterValue = "",
          q = "",
          parameters = {},
          parameterCount = 0,

          str = function(t)
            local s = t.type .. "/" .. t.subtype
            for name,value in pairs(t.parameters) do
              s = s .. "; " .. name .. "=" .. value
            end
            return s
          end,
        }
        v.index = v.index + 1
      end,

      captureTypeChar = function(v,c)
        v.current.type = v.current.type .. c
      end,

      captureSubtypeChar = function(v,c)
        v.current.subtype = v.current.subtype .. c
      end,

      captureQ = function(v,c)
        v.current.q = v.current.q .. c
      end,

      captureParameterName = function(v,c)
        v.current.parameterName = v.current.parameterName .. c
      end,

      captureParameterValue = function(v,c)
        v.current.parameterValue = v.current.parameterValue .. c
      end,

      captureParameter = function(v)
        if v.current.parameters[v.current.parameterName] == nil then
          v.current.parameterCount = v.current.parameterCount + 1
        end
        v.current.parameters[v.current.parameterName] = v.current.parameterValue
        v.current.parameterName = ""
        v.current.parameterValue = ""
      end,

      captureType = function(v)
        v.current.q = tonumber(v.current.q) or 1
        if v.current.q > 0 then table.insert(types,v.current) end
      end,

      captureQ = function(v,c)
        v.current.q = v.current.q .. c
      end
    }

    -- Parse the value character-by-character then cleanly terminate parsing
    for c in header:gmatch('.') do
      values.currentState = conneg.accept._step(values.currentState,values,c)
    end
    conneg.accept._terminate[values.currentState](values)

    -- Sort parsed values by descending preference
    conneg.accept._sort(types)

    -- Return the parsed types
    return types
  end
}
